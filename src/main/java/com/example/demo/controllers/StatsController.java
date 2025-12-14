package com.example.demo.controllers;

import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.model.Products;
import com.example.demo.repository.*;

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

        //Tổng thống kê
        model.addAttribute("totalOrders", ordersRepo.count());
        model.addAttribute("totalProducts", productRepo.count());
        model.addAttribute("totalAccounts", accountRepo.count());
        model.addAttribute("totalCategories", categoryRepo.count());

        //Sản phẩm được yêu thích nhất
        Products topWish = wishlistRepo.findTopByMostLiked().orElse(null);
        model.addAttribute("topWish", topWish);

        //Sản phẩm bán chạy nhất
        Products topSold = ordersRepo.findTopSellingProduct().orElse(null);
        model.addAttribute("topSold", topSold);

        //Số đơn theo tháng (bảng)
        List<Object[]> ordersPerMonth = ordersRepo.countOrdersPerMonth();
        List<String> months = ordersPerMonth.stream()
                .map(r -> "Tháng " + r[0].toString())
                .collect(Collectors.toList());

        List<Long> orderCounts = ordersPerMonth.stream()
                .map(r -> Long.parseLong(r[1].toString()))
                .collect(Collectors.toList());

        model.addAttribute("months", months);
        model.addAttribute("orderCounts", orderCounts);

        //Số đơn tháng hiện tại & năm hiện tại
        YearMonth current = YearMonth.now();

        Long monthOrders = ordersRepo.countOrdersInMonth(current.getMonthValue());
        Long yearOrders = ordersRepo.countOrdersInYear(current.getYear());

        model.addAttribute("monthOrders", monthOrders != null ? monthOrders : 0);
        model.addAttribute("yearOrders", yearOrders != null ? yearOrders : 0);

        return "/admin/stats";
    }
}