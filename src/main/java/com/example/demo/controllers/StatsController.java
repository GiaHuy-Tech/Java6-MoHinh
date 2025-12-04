package com.example.demo.controllers;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.model.Products;
import com.example.demo.repository.*;

import com.fasterxml.jackson.databind.ObjectMapper;

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

    // ------------------- DASHBOARD -------------------
    @GetMapping("/stats")
    public String dashboard(Model model) throws Exception {

        // ✅ Tổng thống kê
        model.addAttribute("totalOrders", ordersRepo.count());
        model.addAttribute("totalProducts", productRepo.count());
        model.addAttribute("totalAccounts", accountRepo.count());
        model.addAttribute("totalCategories", categoryRepo.count());

        // ✅ Sản phẩm được yêu thích nhất
        Products topWish = wishlistRepo.findTopByMostLiked().orElse(null);
        model.addAttribute("topWish", topWish);

        // ✅ Sản phẩm bán chạy nhất
        Products topSold = ordersRepo.findTopSellingProduct().orElse(null);
        model.addAttribute("topSold", topSold);

        // ✅ Doanh thu theo tháng (cho bảng)
        List<Object[]> revenuePerMonth = ordersRepo.revenuePerMonth();
        List<String> months = revenuePerMonth.stream()
                .map(r -> "Tháng " + r[0].toString())
                .collect(Collectors.toList());
        List<Double> revenues = revenuePerMonth.stream()
                .map(r -> Double.parseDouble(r[1].toString()))
                .collect(Collectors.toList());

        model.addAttribute("months", months);
        model.addAttribute("revenues", revenues);

        // ✅ Doanh thu 4 tháng gần nhất (cho Chart)
        List<String> last4Months = getLast4Months();
        List<Double> last4Revenue = new ArrayList<>();
        for (String m : last4Months) {
            Double sum = ordersRepo.getRevenueByMonthLabel(m);
            last4Revenue.add(sum != null ? sum : 0.0);
        }

        ObjectMapper mapper = new ObjectMapper();
        model.addAttribute("labelsJson", mapper.writeValueAsString(last4Months));
        model.addAttribute("dataJson", mapper.writeValueAsString(last4Revenue));

        // ✅ Doanh thu tháng hiện tại và năm hiện tại
        YearMonth current = YearMonth.now();
        Double monthRevenue = ordersRepo.getRevenueByMonth(current.getMonthValue());
        Double yearRevenue = ordersRepo.getRevenueByYear(current.getYear());

        model.addAttribute("monthRevenue", monthRevenue != null ? monthRevenue : 0);
        model.addAttribute("yearRevenue", yearRevenue != null ? yearRevenue : 0);

        return "/admin/stats";
    }

    // ------------------- HÀM HỖ TRỢ -------------------

    // Lấy danh sách 4 tháng gần nhất
    private List<String> getLast4Months() {
        LocalDate now = LocalDate.now();
        List<String> months = new ArrayList<>();
        for (int i = 3; i >= 0; i--) {
            LocalDate date = now.minusMonths(i);
            months.add("Tháng " + date.getMonthValue());
        }
        return months;
    }
}
