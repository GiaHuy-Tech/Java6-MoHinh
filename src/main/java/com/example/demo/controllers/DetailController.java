package com.example.demo.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.*;
import com.example.demo.repository.*;

import jakarta.servlet.http.HttpSession;

@Controller
public class DetailController {

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private VoucherRepository voucherRepo;

    @Autowired
    private CommentRepository commentRepo;
    
    @Autowired
    private HttpSession session; // ✅ Cần session để biết ai đang đăng nhập

    @GetMapping("/product-detail/{id}")
    public String productDetail(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer voucherId,
            Model model) {

        // 1. Lấy thông tin người dùng hiện tại
        Account currentAccount = (Account) session.getAttribute("account");

        // 2. Lấy sản phẩm
        Products product = productRepo.findById(id).orElse(null);
        if (product == null) return "redirect:/products";

        double finalPrice = product.getPrice();
        Voucher selectedVoucher = null;

        // 3. XỬ LÝ LOGIC ÁP DỤNG VOUCHER
        if (voucherId != null) {
            selectedVoucher = voucherRepo.findById(voucherId).orElse(null);

            if (selectedVoucher != null) {
                // Check 1: Phải Active và Chưa hết hạn
                boolean isActive = Boolean.TRUE.equals(selectedVoucher.getActive());
                boolean notExpired = selectedVoucher.getExpiredAt().isAfter(LocalDateTime.now());
                
                // Check 2: LOGIC MỚI - Kiểm tra chủ sở hữu Voucher
                boolean isOwner = true;
                if (selectedVoucher.getAccount() != null) {
                    // Nếu voucher có gán chủ, thì phải đăng nhập và đúng ID mới được dùng
                    if (currentAccount == null || !currentAccount.getId().equals(selectedVoucher.getAccount().getId())) {
                        isOwner = false;
                    }
                }

                // Nếu thỏa mãn tất cả điều kiện thì mới tính tiền
                if (isActive && notExpired && isOwner) {
                    // Tính giảm giá
                    if (selectedVoucher.getDiscountPercent() != null) {
                        finalPrice -= product.getPrice() * selectedVoucher.getDiscountPercent() / 100.0;
                    } else if (selectedVoucher.getDiscountAmount() != null) {
                        finalPrice -= selectedVoucher.getDiscountAmount();
                    }
                } else {
                    // Nếu không thỏa mãn (ví dụ voucher của người khác), hủy chọn voucher
                    selectedVoucher = null;
                }
            }
        }

        if (finalPrice < 0) finalPrice = 0;

        // 4. LỌC DANH SÁCH VOUCHER ĐỂ HIỂN THỊ
        // Chỉ hiện: Voucher chung (account == null) HOẶC Voucher của chính user đó
        List<Voucher> allActiveVouchers = voucherRepo.findByActiveTrueAndExpiredAtAfter(LocalDateTime.now());
        
        List<Voucher> visibleVouchers = allActiveVouchers.stream()
            .filter(v -> v.getAccount() == null || (currentAccount != null && v.getAccount().getId().equals(currentAccount.getId())))
            .collect(Collectors.toList());

        // 5. Đẩy dữ liệu ra View
        model.addAttribute("product", product);
        model.addAttribute("finalPrice", finalPrice);
        model.addAttribute("selectedVoucher", selectedVoucher);
        model.addAttribute("vouchers", visibleVouchers); // Danh sách đã lọc
        model.addAttribute("comments", commentRepo.findByProduct_IdOrderByCreatedDateDesc(id));

        return "client/product-detail";
    }
}