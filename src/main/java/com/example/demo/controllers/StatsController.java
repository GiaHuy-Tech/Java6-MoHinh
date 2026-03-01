package com.example.demo.controllers;

import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.model.Products;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.LikeRepository;
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

    @GetMapping("/stats")
    public String dashboard(Model model) {

        // ===== TỔNG THỐNG KÊ =====
        model.addAttribute("totalOrders", ordersRepo.count());
        model.addAttribute("totalProducts", productRepo.count());
        model.addAttribute("totalAccounts", accountRepo.count());
        model.addAttribute("totalCategories", categoryRepo.count());

        // ===== SẢN PHẨM ĐƯỢC YÊU THÍCH NHẤT =====
        Products topWish = wishlistRepo.findTopByMostLiked().orElse(null);
        model.addAttribute("topWish", topWish);

        // ===== SẢN PHẨM BÁN CHẠY NHẤT =====
        List<Products> topSellingList =
                ordersRepo.findTopSellingProduct(PageRequest.of(0, 1));

        Products topSold = topSellingList.isEmpty()
                ? null
                : topSellingList.get(0);

        model.addAttribute("topSold", topSold);

        // ===== SỐ ĐƠN THEO THÁNG =====
        List<Object[]> ordersPerMonth = ordersRepo.countOrdersPerMonth();

        List<String> months = ordersPerMonth.stream()
                .map(r -> "Tháng " + r[0])
                .collect(Collectors.toList());

        List<Long> orderCounts = ordersPerMonth.stream()
                .map(r -> ((Number) r[1]).longValue())
                .collect(Collectors.toList());

        model.addAttribute("months", months);
        model.addAttribute("orderCounts", orderCounts);

        // ===== THÁNG HIỆN TẠI & NĂM HIỆN TẠI =====
        YearMonth current = YearMonth.now();

        Long monthOrders =
                ordersRepo.countOrdersByMonth(current.getMonthValue());

        Long yearOrders =
                ordersRepo.countOrdersInYear(current.getYear());

        model.addAttribute("monthOrders",
                monthOrders != null ? monthOrders : 0);

        model.addAttribute("yearOrders",
                yearOrders != null ? yearOrders : 0);

        return "admin/stats";
    }
}