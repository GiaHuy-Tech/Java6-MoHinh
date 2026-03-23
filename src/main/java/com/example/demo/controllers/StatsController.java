package com.example.demo.controllers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

        int currentYear = (year != null) ? year : LocalDate.now().getYear();

        LocalDate from = (fromDate != null && !fromDate.isEmpty())
                ? LocalDate.parse(fromDate) : null;

        LocalDate to = (toDate != null && !toDate.isEmpty())
                ? LocalDate.parse(toDate) : null;

        boolean isFilterByDate = (from != null || to != null);

        model.addAttribute("selectedYear", currentYear);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        // ===== YEAR LIST =====
        List<Integer> years = new ArrayList<>();
        for (int i = 2020; i <= LocalDate.now().getYear(); i++) {
            years.add(i);
        }
        model.addAttribute("years", years);

        // ===== COUNT =====
        Long totalOrders = isFilterByDate
                ? ordersRepo.countCompletedOrdersByDate(from, to)
                : ordersRepo.countOrdersPerMonthByYear(currentYear)
                    .stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();

        model.addAttribute("totalOrders", totalOrders != null ? totalOrders : 0);
        model.addAttribute("totalProducts", productRepo.count());
        model.addAttribute("totalAccounts", accountRepo.count());
        model.addAttribute("totalCategories", categoryRepo.count());

        // ===== REVENUE =====
        Long totalRevenue = isFilterByDate
                ? ordersRepo.getTotalRevenueByDate(from, to)
                : ordersRepo.getRevenueByMonth(currentYear)
                    .stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();

        model.addAttribute("totalRevenue", totalRevenue != null ? totalRevenue : 0);

        // ===== CATEGORY =====
        List<Object[]> revenueByCategory = isFilterByDate
                ? ordersRepo.getRevenueByCategoryByDate(from, to)
                : ordersRepo.getRevenueByCategory();

        List<String> chartLabels = new ArrayList<>();
        List<Long> chartData = new ArrayList<>();

        for (Object[] row : revenueByCategory) {
            chartLabels.add((String) row[0]);
            chartData.add(((Number) row[1]).longValue());
        }

        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);

        // ===== MONTH CHART =====
        List<Object[]> revenueByMonth = isFilterByDate
                ? ordersRepo.getRevenueByMonthByDate(from, to)
                : ordersRepo.getRevenueByMonth(currentYear);

        List<Long> monthlyRevenueData = new ArrayList<>(Collections.nCopies(12, 0L));
        for (Object[] row : revenueByMonth) {
            int m = ((Number) row[0]).intValue();
            monthlyRevenueData.set(m - 1, ((Number) row[1]).longValue());
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

        // ===== ORDERS PER MONTH =====
        List<Object[]> ordersPerMonth = isFilterByDate
                ? ordersRepo.countOrdersPerMonthByDate(from, to)
                : ordersRepo.countOrdersPerMonthByYear(currentYear);

        List<Long> orderCounts = new ArrayList<>(Collections.nCopies(12, 0L));
        for (Object[] row : ordersPerMonth) {
            int m = ((Number) row[0]).intValue();
            orderCounts.set(m - 1, ((Number) row[1]).longValue());
        }

        List<String> months = new ArrayList<>();
        for (int i = 1; i <= 12; i++) months.add("Tháng " + i);

        model.addAttribute("months", months);
        model.addAttribute("orderCounts", orderCounts);

        int currentMonth = LocalDate.now().getMonthValue();
        model.addAttribute("monthOrders", orderCounts.get(currentMonth - 1));
        model.addAttribute("yearOrders", totalOrders);

        // ===== MEMBERSHIP =====
        model.addAttribute("memberships", membershipRepo.findAll());

        List<Object[]> memStats = membershipRepo.countUsersByMembership();
        List<String> memLabels = new ArrayList<>();
        List<Long> memData = new ArrayList<>();

        for (Object[] row : memStats) {
            memLabels.add((String) row[0]);
            memData.add(((Number) row[1]).longValue());
        }

        model.addAttribute("memLabels", memLabels);
        model.addAttribute("memData", memData);

        return "admin/stats";
    }
}