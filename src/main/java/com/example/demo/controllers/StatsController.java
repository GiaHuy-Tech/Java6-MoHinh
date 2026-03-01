package com.example.demo.controllers;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
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

        // ===== 1. TỔNG QUAN (TOP CARDS) =====
        // Sử dụng hàm đếm đơn hoàn tất thay vì count() tất cả
        model.addAttribute("totalOrders", ordersRepo.countCompletedOrders()); 
        model.addAttribute("totalProducts", productRepo.count());
        model.addAttribute("totalAccounts", accountRepo.count());
        model.addAttribute("totalCategories", categoryRepo.count());

        // ===== 2. TỔNG DOANH THU TOÀN THỜI GIAN =====
        Long totalRev = ordersRepo.getTotalRevenue();
        model.addAttribute("totalRevenue", totalRev != null ? totalRev : 0L);

        // ===== 3. DỮ LIỆU BIỂU ĐỒ TRÒN (DOANH THU THEO DANH MỤC) =====
        List<Object[]> revenueByCategory = ordersRepo.getRevenueByCategory();
        
        List<String> chartLabels = new ArrayList<>();
        List<Long> chartData = new ArrayList<>();

        if (revenueByCategory != null) {
            for (Object[] row : revenueByCategory) {
                chartLabels.add((String) row[0]);             // Tên danh mục
                chartData.add(((Number) row[1]).longValue()); // Doanh thu
            }
        }
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);


        // ===== 4. DỮ LIỆU BIỂU ĐỒ CỘT (DOANH THU THEO 12 THÁNG) =====
        int currentYear = LocalDate.now().getYear();
        List<Object[]> revenueByMonth = ordersRepo.getRevenueByMonth(currentYear);

        // Tạo mảng 12 phần tử có giá trị 0 (đại diện tháng 1 -> 12)
        List<Long> monthlyRevenueData = new ArrayList<>(Collections.nCopies(12, 0L));
        List<String> monthlyLabels = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            monthlyLabels.add("Tháng " + i);
        }

        // Đổ dữ liệu từ DB vào đúng vị trí tháng
        if (revenueByMonth != null) {
            for (Object[] row : revenueByMonth) {
                int month = (int) row[0];
                Long revenue = ((Number) row[1]).longValue();
                // List index bắt đầu từ 0, tháng bắt đầu từ 1 -> phải trừ 1
                if (month >= 1 && month <= 12) {
                    monthlyRevenueData.set(month - 1, revenue);
                }
            }
        }
        model.addAttribute("revenueMonthLabels", monthlyLabels);
        model.addAttribute("revenueMonthData", monthlyRevenueData);


        // ===== 5. TOP SẢN PHẨM (YÊU THÍCH & BÁN CHẠY) =====
        Products topWish = wishlistRepo.findTopByMostLiked().orElse(null);
        model.addAttribute("topWish", topWish);

        List<Products> topSellingList = ordersRepo.findTopSellingProduct(PageRequest.of(0, 1));
        Products topSold = topSellingList.isEmpty() ? null : topSellingList.get(0);
        model.addAttribute("topSold", topSold);


        // ===== 6. BẢNG THỐNG KÊ SỐ ĐƠN (DƯỚI CÙNG) =====
        List<Object[]> ordersPerMonth = ordersRepo.countOrdersPerMonth();
        List<String> months = new ArrayList<>();
        List<Long> orderCounts = new ArrayList<>();

        if (ordersPerMonth != null) {
            for (Object[] row : ordersPerMonth) {
                 months.add("Tháng " + row[0]);
                 orderCounts.add(((Number) row[1]).longValue());
            }
        }
        model.addAttribute("months", months);
        model.addAttribute("orderCounts", orderCounts);
        
        // Thêm số liệu đơn tháng này/năm này cho thẻ hiển thị
        // (Bạn có thể viết thêm hàm countCompletedOrdersByMonth ở Repo để chính xác hơn)
        YearMonth current = YearMonth.now();
        model.addAttribute("monthOrders", orderCounts.isEmpty() ? 0 : orderCounts.get(orderCounts.size() -1)); // Lấy tạm tháng cuối
        model.addAttribute("yearOrders", ordersRepo.countCompletedOrders()); // Lấy tổng năm

        return "admin/stats";
    }
}