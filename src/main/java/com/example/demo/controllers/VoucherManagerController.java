package com.example.demo.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Membership;
import com.example.demo.model.Voucher;
import com.example.demo.repository.MembershipRepository; // Giả sử bạn đã tạo repo này
import com.example.demo.repository.VoucherRepository;

@Controller
@RequestMapping("/admin/vouchers")
public class VoucherManagerController {

    @Autowired
    private VoucherRepository voucherRepo;

    @Autowired
    private MembershipRepository membershipRepo; // Cần thêm Repo này

    // 1. HIỆN DANH SÁCH + FORM
    @GetMapping
    public String listVouchers(Model model) {
        return loadPage(model, new Voucher());
    }

    // 2. CHẾ ĐỘ SỬA
    @GetMapping("/edit/{id}")
    public String editVoucher(@PathVariable("id") Integer id, Model model) {
        Voucher voucher = voucherRepo.findById(id).orElse(new Voucher());
        return loadPage(model, voucher);
    }

    private String loadPage(Model model, Voucher voucher) {
        List<Voucher> list = voucherRepo.findAll();
        List<Membership> memberships = membershipRepo.findAll(); // Lấy list hạng thành viên

        model.addAttribute("vouchers", list);
        model.addAttribute("memberships", memberships);
        model.addAttribute("voucher", voucher);

        return "admin/voucher-list"; 
    }

    // 3. LƯU (THÊM HOẶC SỬA)
    @PostMapping("/save")
    public String saveVoucher(@ModelAttribute("voucher") Voucher voucher,
                              @RequestParam(name = "membershipId", required = false) Integer membershipId) {

        // 1. Xử lý Logic FreeShipping
        if (Boolean.TRUE.equals(voucher.getIsFreeShipping())) {
            // Nếu là FreeShip thì reset các giá trị giảm tiền về null/0
            voucher.setDiscountPercent(null);
            voucher.setDiscountAmount(null);
        }

        // 2. Xử lý Membership
        if (membershipId != null && membershipId > 0) {
            Membership mem = membershipRepo.findById(membershipId).orElse(null);
            voucher.setMembership(mem);
        } else {
            voucher.setMembership(null); // Tất cả thành viên
        }

        // 3. Xử lý Active mặc định
        if (voucher.getId() == null) {
            voucher.setActive(true);
        }
        
        // 4. Ngày hết hạn mặc định
        if (voucher.getExpiredAt() == null) {
            voucher.setExpiredAt(LocalDateTime.now().plusMonths(1));
        }

        // 5. Xử lý logic Birthday (đã map tự động qua @ModelAttribute nếu checkbox có name="isBirthday")

        voucherRepo.save(voucher);
        return "redirect:/admin/vouchers";
    }

    // 4. ẨN VOUCHER
    @GetMapping("/hide/{id}")
    public String hideVoucher(@PathVariable("id") Integer id) {
        voucherRepo.findById(id).ifPresent(v -> {
            v.setActive(false);
            voucherRepo.save(v);
        });
        return "redirect:/admin/vouchers";
    }

    // 5. HIỆN VOUCHER
    @GetMapping("/show/{id}")
    public String showVoucher(@PathVariable("id") Integer id) {
        voucherRepo.findById(id).ifPresent(v -> {
            v.setActive(true);
            voucherRepo.save(v);
        });
        return "redirect:/admin/vouchers";
    }
}