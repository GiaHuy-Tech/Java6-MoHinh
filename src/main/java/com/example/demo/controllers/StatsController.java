package com.example.demo.controllers;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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

    @Autowired
    private OrdersRepository ordersRepo;

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private CategoryRepository categoryRepo;

    @Autowired
    private LikeRepository wishlistRepo;

    @Autowired
    private MembershipRepository membershipRepo; // Đã thêm repo này

    // ===== TRANG DASHBOARD THỐNG KÊ =====
    @GetMapping("/stats")
    public String dashboard(Model model,
                            @RequestParam(value = "year", required = false) Integer year) {

        int currentYear = (year != null) ? year : LocalDate.now().getYear();
        model.addAttribute("selectedYear", currentYear);

        List<Integer> years = new ArrayList<>();
        for (int i = 2020; i <= LocalDate.now().getYear(); i++) {
            years.add(i);
        }
        model.addAttribute("years", years);

        model.addAttribute("totalOrders", ordersRepo.countCompletedOrders());
        model.addAttribute("totalProducts", productRepo.count());
        model.addAttribute("totalAccounts", accountRepo.count());
        model.addAttribute("totalCategories", categoryRepo.count());

        Long totalRev = ordersRepo.getTotalRevenue();
        model.addAttribute("totalRevenue", totalRev != null ? totalRev : 0L);

        // ===== Chart danh mục =====
        List<Object[]> revenueByCategory = ordersRepo.getRevenueByCategory();
        List<String> chartLabels = new ArrayList<>();
        List<Long> chartData = new ArrayList<>();

        for (Object[] row : revenueByCategory) {
            chartLabels.add((String) row[0]);
            chartData.add(((Number) row[1]).longValue());
        }

        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);

        // ===== Chart tháng =====
        List<Object[]> revenueByMonth = ordersRepo.getRevenueByMonth(currentYear);

        List<Long> monthlyRevenueData = new ArrayList<>(Collections.nCopies(12, 0L));
        List<String> monthlyLabels = new ArrayList<>();

        for (int i = 1; i <= 12; i++) {
            monthlyLabels.add("Tháng " + i);
        }

        for (Object[] row : revenueByMonth) {
            int month = (int) row[0];
            Long revenue = ((Number) row[1]).longValue();
            monthlyRevenueData.set(month - 1, revenue);
        }

        model.addAttribute("revenueMonthLabels", monthlyLabels);
        model.addAttribute("revenueMonthData", monthlyRevenueData);

        // ===== Top =====
        Products topWish = wishlistRepo.findTopByMostLiked().orElse(null);
        model.addAttribute("topWish", topWish);

        List<Products> topSellingList = ordersRepo.findTopSellingProduct(PageRequest.of(0, 1));
        model.addAttribute("topSold", topSellingList.isEmpty() ? null : topSellingList.get(0));

        // ===== Đơn theo tháng =====
        List<Object[]> ordersPerMonth = ordersRepo.countOrdersPerMonthByYear(currentYear);

        List<String> months = new ArrayList<>();
        List<Long> orderCounts = new ArrayList<>();

        for (Object[] row : ordersPerMonth) {
            months.add("Tháng " + row[0]);
            orderCounts.add(((Number) row[1]).longValue());
        }

        model.addAttribute("months", months);
        model.addAttribute("orderCounts", orderCounts);

        model.addAttribute("monthOrders", orderCounts.isEmpty() ? 0 : orderCounts.get(orderCounts.size() - 1));
        model.addAttribute("yearOrders", ordersRepo.countCompletedOrders());

        // ===== Membership =====
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