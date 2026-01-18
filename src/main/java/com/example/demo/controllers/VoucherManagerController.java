package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Account;
import com.example.demo.model.Voucher;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.VoucherRepository;

@Controller
@RequestMapping("/admin/vouchers")
public class VoucherManagerController {

    @Autowired
    private VoucherRepository voucherRepo;

    @Autowired
    private AccountRepository accountRepo;

    // 1. HIỆN DANH SÁCH + FORM RỖNG (CHẾ ĐỘ THÊM MỚI)
    @GetMapping
    public String listVouchers(Model model) {
        model.addAttribute("vouchers", voucherRepo.findAll()); // Dữ liệu cho bảng
        model.addAttribute("accounts", accountRepo.findAll()); // Dữ liệu cho dropdown
        model.addAttribute("voucher", new Voucher()); // Form rỗng
        return "admin/voucher-list"; 
    }

    // 2. HIỆN DANH SÁCH + FORM CÓ DỮ LIỆU (CHẾ ĐỘ SỬA)
    @GetMapping("/edit/{id}")
    public String editVoucher(@PathVariable Integer id, Model model) {
        Voucher voucher = voucherRepo.findById(id).orElse(new Voucher());
        
        model.addAttribute("vouchers", voucherRepo.findAll()); // Vẫn hiện bảng danh sách
        model.addAttribute("accounts", accountRepo.findAll());
        model.addAttribute("voucher", voucher); // Form điền dữ liệu cũ
        
        return "admin/voucher-list"; // Trả về cùng 1 trang html
    }

    // 3. LƯU (THÊM HOẶC SỬA)
    @PostMapping("/save")
    public String saveVoucher(@ModelAttribute("voucher") Voucher voucher,
                              @RequestParam(name = "accountId", required = false) Integer accountId) {
        
        // Xử lý Account thủ công
        if (accountId != null) {
            Account acc = accountRepo.findById(accountId).orElse(null);
            voucher.setAccount(acc);
        } else {
            voucher.setAccount(null);
        }

        voucherRepo.save(voucher);
        return "redirect:/admin/vouchers"; // Lưu xong quay về chế độ thêm mới
    }

    // 4. XÓA
    @GetMapping("/hide/{id}")
    public String hideVoucher(@PathVariable Integer id) {
        voucherRepo.findById(id).ifPresent(voucher -> {
            voucher.setActive(false); // ẨN
            voucherRepo.save(voucher);
        });
        return "redirect:/admin/vouchers";
    }
    @GetMapping("/show/{id}")
    public String showVoucher(@PathVariable Integer id) {
        voucherRepo.findById(id).ifPresent(voucher -> {
            voucher.setActive(true); // HIỆN
            voucherRepo.save(voucher);
        });
        return "redirect:/admin/vouchers";
    }


}