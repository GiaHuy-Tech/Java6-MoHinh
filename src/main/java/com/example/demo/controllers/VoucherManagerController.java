package com.example.demo.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Membership;
import com.example.demo.model.Voucher;
import com.example.demo.repository.MembershipRepository;
import com.example.demo.repository.VoucherRepository;

@Controller
@RequestMapping("/admin/vouchers")
public class VoucherManagerController {

    @Autowired
    private VoucherRepository voucherRepo;

    @Autowired
    private MembershipRepository membershipRepo;

    // 1. HIỆN DANH SÁCH + FORM + TÌM KIẾM & LỌC
    @GetMapping
    public String listVouchers(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "discountType", required = false) String discountType,
            @RequestParam(name = "filterMembershipId", required = false) Integer filterMembershipId,
            @RequestParam(name = "status", required = false) String status,
            Model model) {
        
        return loadPage(model, new Voucher(), keyword, discountType, filterMembershipId, status);
    }

    // 2. CHẾ ĐỘ SỬA
    @GetMapping("/edit/{id}")
    public String editVoucher(@PathVariable("id") Integer id, Model model) {
        Voucher voucher = voucherRepo.findById(id).orElse(new Voucher());
        // Khi đang ở chế độ sửa, tạm thời reset các bộ lọc
        return loadPage(model, voucher, null, null, null, null);
    }

    // HÀM HỖ TRỢ XỬ LÝ LOAD TRANG VÀ LỌC DỮ LIỆU BẰNG JAVA STREAM
    private String loadPage(Model model, Voucher voucher, String keyword, String discountType, Integer filterMembershipId, String status) {
        List<Voucher> list = voucherRepo.findAll();

        // --- BẮT ĐẦU XỬ LÝ LỌC ---

        // Lọc theo từ khóa (Mã Code)
        if (keyword != null && !keyword.trim().isEmpty()) {
            list = list.stream()
                    .filter(v -> v.getCode() != null && v.getCode().toLowerCase().contains(keyword.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Lọc theo loại giảm giá
        if (discountType != null && !discountType.isEmpty()) {
            if ("percent".equals(discountType)) {
                list = list.stream().filter(v -> v.getDiscountPercent() != null).collect(Collectors.toList());
            } else if ("amount".equals(discountType)) {
                list = list.stream().filter(v -> v.getDiscountAmount() != null).collect(Collectors.toList());
            } else if ("freeship".equals(discountType)) {
                list = list.stream().filter(v -> Boolean.TRUE.equals(v.getIsFreeShipping())).collect(Collectors.toList());
            }
        }

        // Lọc theo hạng thành viên (Membership)
        if (filterMembershipId != null) {
            if (filterMembershipId == 0) {
                // Lọc các voucher áp dụng cho tất cả (Membership == null)
                list = list.stream().filter(v -> v.getMembership() == null).collect(Collectors.toList());
            } else {
                // Lọc theo ID Membership cụ thể
                list = list.stream()
                        .filter(v -> v.getMembership() != null && v.getMembership().getId().equals(filterMembershipId))
                        .collect(Collectors.toList());
            }
        }

        // Lọc theo trạng thái
        if (status != null && !status.isEmpty()) {
            if ("active".equals(status)) {
                list = list.stream().filter(v -> Boolean.TRUE.equals(v.getActive())).collect(Collectors.toList());
            } else if ("hidden".equals(status)) {
                list = list.stream().filter(v -> !Boolean.TRUE.equals(v.getActive())).collect(Collectors.toList());
            }
        }
        // --- KẾT THÚC XỬ LÝ LỌC ---

        List<Membership> memberships = membershipRepo.findAll();

        model.addAttribute("vouchers", list);
        model.addAttribute("memberships", memberships);
        model.addAttribute("voucher", voucher);
        
        // Trả lại các tham số lọc về View để giữ trạng thái cho Form lọc
        model.addAttribute("keyword", keyword);
        model.addAttribute("discountType", discountType);
        model.addAttribute("filterMembershipId", filterMembershipId);
        model.addAttribute("status", status);

        return "admin/voucher-list"; 
    }

    // 3. LƯU (THÊM HOẶC SỬA)
    @PostMapping("/save")
    public String saveVoucher(@ModelAttribute("voucher") Voucher voucher,
                              @RequestParam(name = "membershipId", required = false) Integer membershipId) {

        // 1. Xử lý Logic FreeShipping
        if (Boolean.TRUE.equals(voucher.getIsFreeShipping())) {
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