package com.example.demo.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Account;
import com.example.demo.model.Voucher;
import com.example.demo.repository.VoucherRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/voucher")
public class VoucherController {

    @Autowired
    VoucherRepository voucherRepo;

    // ===============================
    // HIỂN THỊ TRANG VOUCHER
    // ===============================
    @GetMapping
    public String voucherPage(Model model, HttpSession session) {

        Account account = (Account) session.getAttribute("account");

        if (account == null) {
            return "redirect:/login";
        }

        List<Voucher> availableVouchers = new ArrayList<>();

        // 1. voucher chung
        List<Voucher> publicVouchers = voucherRepo.findByAccountIsNull();

        // 2. voucher membership
        List<Voucher> membershipVouchers = new ArrayList<>();

        if (account.getMembership() != null) {
            membershipVouchers = voucherRepo.findByMembership_IdAndAccountIsNull(
                    account.getMembership().getId()
            );
        }

        // gộp tất cả lại
        availableVouchers.addAll(publicVouchers);
        availableVouchers.addAll(membershipVouchers);

        model.addAttribute("availableVouchers", availableVouchers);

        return "client/voucher";
    }

    // ===============================
    // LƯU VOUCHER
    // ===============================
    @PostMapping("/claim")
    public String claimVoucher(@RequestParam("voucherId") Integer originalVoucherId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        try {
            Voucher originalVoucher = voucherRepo.findById(originalVoucherId).orElse(null);
            if (originalVoucher == null) {
                redirectAttributes.addFlashAttribute("error", "Voucher không tồn tại");
                return "redirect:/voucher";
            }

            // --- LOGIC KIỂM TRA CẤP BẬC THÀNH VIÊN ---
            if (originalVoucher.getMembership() != null) {
                int userPoints = (account.getMembership() != null) ? account.getMembership().getPointRequired() : 0;
                int requiredPoints = originalVoucher.getMembership().getPointRequired();

                if (userPoints < requiredPoints) {
                    redirectAttributes.addFlashAttribute("error", "Cấp bậc thành viên của bạn chưa đủ để nhận mã này!");
                    return "redirect:/voucher";
                }
            }

            // Kiểm tra đã lưu chưa
            if (voucherRepo.existsByAccount_IdAndCode(account.getId(), originalVoucher.getCode())) {
                redirectAttributes.addFlashAttribute("error", "Bạn đã lưu mã này rồi!");
                return "redirect:/voucher";
            }

            // Tạo voucher riêng cho User
            Voucher newVoucher = Voucher.builder()
                    .code(originalVoucher.getCode())
                    .discountPercent(originalVoucher.getDiscountPercent())
                    .discountAmount(originalVoucher.getDiscountAmount())
                    .minOrderValue(originalVoucher.getMinOrderValue())
                    .expiredAt(originalVoucher.getExpiredAt())
                    .active(true)
                    .isFreeShipping(originalVoucher.getIsFreeShipping())
                    .isBirthday(originalVoucher.getIsBirthday())
                    .membership(originalVoucher.getMembership())
                    .account(account)
                    .build();

            voucherRepo.save(newVoucher);
            redirectAttributes.addFlashAttribute("success", "Lưu voucher thành công!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/voucher";
    }

}