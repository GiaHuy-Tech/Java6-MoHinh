package com.example.demo.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Membership;
import com.example.demo.model.Products;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.LikeRepository;
import com.example.demo.repository.MembershipRepository;
import com.example.demo.repository.OrdersRepository;
import com.example.demo.repository.ProductRepository;

@Controller
public class StatsController {

    @Autowired private OrdersRepository ordersRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private AccountRepository accountRepo;
    @Autowired private CategoryRepository categoryRepo;
    @Autowired private LikeRepository wishlistRepo;
    @Autowired private MembershipRepository membershipRepo;

    @GetMapping("/stats")
    public String dashboard(Model model,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate) {

        // ===== DATETIME & LOGIC TÁCH BIỆT THÔNG SỐ =====
        LocalDateTime from = (fromDate != null && !fromDate.isEmpty()) ? LocalDate.parse(fromDate).atStartOfDay() : null;
        LocalDateTime to = (toDate != null && !toDate.isEmpty()) ? LocalDate.parse(toDate).plusDays(1).atStartOfDay() : null;
        boolean isFilterByDate = (from != null || to != null);
        
        // chartYear: Chỉ áp dụng cho các Biểu Đồ theo năm (Thanh Bar và Tiến độ)
        int chartYear = (year != null) ? year : LocalDate.now().getYear();
        
        // topStatsYear: Giữ tĩnh cho phần thẻ chỉ số Tổng để không bị biến động khi đổi năm của chart 
        int topStatsYear = LocalDate.now().getYear();

        model.addAttribute("selectedYear", chartYear);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        // ===== YEAR LIST =====
        List<Integer> years = new ArrayList<>();
        for (int i = 2020; i <= LocalDate.now().getYear(); i++) {
            years.add(i);
        }
        model.addAttribute("years", years);

        // ===== TOTAL ORDERS (KHÔNG BỊ ẢNH HƯỞNG BỞI CHART YEAR) =====
        Long totalOrders = isFilterByDate
                ? ordersRepo.countCompletedOrdersByDate(from, to)
                : ordersRepo.countOrdersPerMonthByYear(topStatsYear)
                    .stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();

        model.addAttribute("totalOrders", totalOrders != null ? totalOrders : 0);

        // ===== BASIC =====
        model.addAttribute("totalProducts", productRepo.count());
        model.addAttribute("totalAccounts", accountRepo.count());
        model.addAttribute("totalCategories", categoryRepo.count());

        // ===== TOTAL REVENUE (KHÔNG BỊ ẢNH HƯỞNG BỞI CHART YEAR) =====
        BigDecimal totalRevenue = isFilterByDate
                ? ordersRepo.getTotalRevenueByDate(from, to)
                : ordersRepo.getRevenueByMonth(topStatsYear).stream()
                    .map(r -> (BigDecimal) (r[1] != null ? r[1] : BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        model.addAttribute("totalRevenue", totalRevenue);

        // ===== CATEGORY (PIE CHART) =====
        List<Object[]> revenueByCategory = isFilterByDate ? ordersRepo.getRevenueByCategoryByDate(from, to) : ordersRepo.getRevenueByCategory();
        List<String> chartLabels = new ArrayList<>();
        List<BigDecimal> chartData = new ArrayList<>();

        for (Object[] row : revenueByCategory) {
            chartLabels.add((String) row[0]);
            chartData.add(row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
        }
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);

        // ===== MONTHLY REVENUE (CHỈ BỊ ẢNH HƯỞNG BỞI CHART YEAR) =====
        // Lấy độc lập theo Năm của biểu đồ chọn ở Dropdown
        List<Object[]> revenueByMonth = ordersRepo.getRevenueByMonth(chartYear);
        List<BigDecimal> monthlyRevenueData = new ArrayList<>(Collections.nCopies(12, BigDecimal.ZERO));

        for (Object[] row : revenueByMonth) {
            int m = ((Number) row[0]).intValue();
            monthlyRevenueData.set(m - 1, row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
        }

        List<String> monthlyLabels = new ArrayList<>();
        for (int i = 1; i <= 12; i++) monthlyLabels.add("Tháng " + i);
        model.addAttribute("revenueMonthLabels", monthlyLabels);
        model.addAttribute("revenueMonthData", monthlyRevenueData);

        // ===== TOP =====
        Products topWish = wishlistRepo.findTopByMostLiked().orElse(null);
        model.addAttribute("topWish", topWish);

        List<Products> topSelling = ordersRepo.findTopSellingProduct(PageRequest.of(0, 1));
        model.addAttribute("topSold", topSelling.isEmpty() ? null : topSelling.get(0));

        // ===== ORDERS PER MONTH (CHỈ BỊ ẢNH HƯỞNG BỞI CHART YEAR) =====
        List<Object[]> ordersPerMonth = ordersRepo.countOrdersPerMonthByYear(chartYear);
        List<Long> orderCounts = new ArrayList<>(Collections.nCopies(12, 0L));

        for (Object[] row : ordersPerMonth) {
            int m = ((Number) row[0]).intValue();
            orderCounts.set(m - 1, ((Number) row[1]).longValue());
        }
        model.addAttribute("months", monthlyLabels);
        model.addAttribute("orderCounts", orderCounts);

        int currentMonth = LocalDate.now().getMonthValue();
        model.addAttribute("monthOrders", orderCounts.get(currentMonth - 1));
        
        // Tính tổng đơn hàng cụ thể của năm đã chọn để fill màu phần trăm (% progress bar)
        long chartYearTotalOrders = orderCounts.stream().mapToLong(Long::longValue).sum();
        model.addAttribute("chartYearTotalOrders", chartYearTotalOrders);

        // ===== MEMBERSHIP STATS & MANAGEMENT =====
        List<Membership> memberships = membershipRepo.findAll(Sort.by(Sort.Direction.ASC, "pointRequired"));
        model.addAttribute("memberships", memberships);

        List<Object[]> memStats = membershipRepo.countUsersByMembership();
        Map<String, Long> membershipUserCounts = new HashMap<>();
        
        for (Object[] row : memStats) {
            String memName = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            membershipUserCounts.put(memName, count);
        }
        model.addAttribute("membershipUserCounts", membershipUserCounts);

        return "admin/stats";
    }

    // ===== API ĐỂ CHỈNH SỬA QUYỀN LỢI HẠNG THÀNH VIÊN =====
    @PostMapping("/admin/membership/update")
    public String updateMembership(
            @RequestParam("id") Integer id,
            @RequestParam("pointRequired") Integer pointRequired,
            @RequestParam("discount") Integer discount,
            @RequestParam(value = "freeShipping", required = false) Boolean freeShipping) {
        
        Membership m = membershipRepo.findById(id).orElse(null);
        if (m != null) {
            m.setPointRequired(pointRequired);
            m.setDiscount(discount);
            m.setFreeShipping(freeShipping != null ? freeShipping : false);
            membershipRepo.save(m);
        }
        return "redirect:/stats";
    }
}