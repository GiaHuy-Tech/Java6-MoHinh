package com.example.demo.controllers;

import com.example.demo.model.Account;
import com.example.demo.model.Voucher;
import com.example.demo.repository.VoucherRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/voucher")
public class VoucherController {

    @Autowired
    VoucherRepository voucherRepo;

    @GetMapping
    public String getVoucherPage(Model model, HttpSession session) {
        // 1. Kiểm tra đăng nhập
        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login"; 
        }

        // 2. Lấy thời gian thực
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        // 3. Lấy danh sách Voucher của tôi 
        // SỬA: Gọi hàm có dấu gạch dưới Account_Id
        List<Voucher> myVouchers = voucherRepo.findByAccount_IdOrderByIdDesc(account.getId());

        // 4. Lấy danh sách Voucher chung (để săn)
        List<Voucher> allPublicVouchers = voucherRepo.findByAccountIsNullAndActiveTrueAndExpiredAtAfter(now);

        // --- LOGIC PHÂN LOẠI HIỂN THỊ (Sinh nhật vs Thường) ---
        List<Voucher> birthdayVouchers = new ArrayList<>();
        boolean isBirthdayMonth = false;

        // Kiểm tra nếu user có ngày sinh nhật
        if (account.getBirthDay() != null) {
            LocalDate birthDate = account.getBirthDay(); 
            
            // Nếu tháng hiện tại trùng tháng sinh
            if (birthDate.getMonth() == today.getMonth()) {
                isBirthdayMonth = true;
                // Lọc voucher có code bắt đầu bằng HPBD hoặc BIRTHDAY
                birthdayVouchers = allPublicVouchers.stream()
                        .filter(v -> v.getCode().toUpperCase().startsWith("HPBD") 
                                  || v.getCode().toUpperCase().startsWith("BIRTHDAY"))
                        .collect(Collectors.toList());
            }
        }

        // Lọc voucher thường (Không phải sinh nhật)
        List<Voucher> availableVouchers = allPublicVouchers.stream()
                .filter(v -> !v.getCode().toUpperCase().startsWith("HPBD") 
                          && !v.getCode().toUpperCase().startsWith("BIRTHDAY"))
                .collect(Collectors.toList());

        // Đẩy dữ liệu ra view
        model.addAttribute("myVouchers", myVouchers);
        model.addAttribute("availableVouchers", availableVouchers);
        model.addAttribute("birthdayVouchers", birthdayVouchers);
        model.addAttribute("isBirthdayMonth", isBirthdayMonth);

        return "client/voucher"; 
    }

    @PostMapping("/claim")
    public String claimVoucher(@RequestParam("voucherId") Integer originalVoucherId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        
        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        try {
            // Tìm voucher gốc
            Voucher originalVoucher = voucherRepo.findByIdAndAccountIsNull(originalVoucherId)
                    .orElseThrow(() -> new Exception("Voucher không tồn tại hoặc đã hết hạn!"));

            // Kiểm tra xem đã lưu mã này chưa
            // SỬA: Gọi hàm có dấu gạch dưới Account_Id
            boolean alreadyHas = voucherRepo.existsByAccount_IdAndCode(account.getId(), originalVoucher.getCode());
            
            if (alreadyHas) {
                redirectAttributes.addFlashAttribute("error", "Bạn đã lưu mã này rồi!");
                return "redirect:/voucher";
            }

            // Tạo bản sao voucher cho user
            Voucher newVoucher = Voucher.builder()
                    .code(originalVoucher.getCode()) // Giữ nguyên code
                    .discountPercent(originalVoucher.getDiscountPercent())
                    .discountAmount(originalVoucher.getDiscountAmount())
                    .minOrderValue(originalVoucher.getMinOrderValue())
                    .expiredAt(originalVoucher.getExpiredAt())
                    .active(true)
                    .account(account) // Gán cho user hiện tại
                    .build();

            voucherRepo.save(newVoucher);
            redirectAttributes.addFlashAttribute("success", "Lưu voucher thành công!");

        } catch (Exception e) {
            e.printStackTrace(); 
            // Nếu lỗi do trùng code (Duplicate entry) thì thông báo khéo
            if(e.getMessage().contains("Duplicate entry")) {
                redirectAttributes.addFlashAttribute("error", "Bạn đã sở hữu mã này rồi (Lỗi trùng lặp).");
            } else {
                redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            }
        }

        return "redirect:/voucher";
    }
}