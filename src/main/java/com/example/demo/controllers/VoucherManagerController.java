package com.example.demo.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
        // Khi đang sửa, reset bộ lọc để dễ quan sát đối tượng đang sửa
        return loadPage(model, voucher, null, null, null, null);
    }

    // HÀM HỖ TRỢ XỬ LÝ LOAD TRANG VÀ LỌC DỮ LIỆU BẰNG JAVA STREAM
    private String loadPage(Model model, Voucher voucher, String keyword, String discountType, Integer filterMembershipId, String status) {
        List<Voucher> list = voucherRepo.findAll();
        LocalDateTime now = LocalDateTime.now();

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
                list = list.stream().filter(v -> v.getMembership() == null).collect(Collectors.toList());
            } else {
                list = list.stream()
                        .filter(v -> v.getMembership() != null && v.getMembership().getId().equals(filterMembershipId))
                        .collect(Collectors.toList());
            }
        }

        // Lọc theo trạng thái THỰC TẾ (Kết hợp active và expiredAt)
        if (status != null && !status.isEmpty()) {
            if ("active".equals(status)) {
                // Chỉ lấy voucher đang bật và CÒN HẠN
                list = list.stream()
                    .filter(v -> Boolean.TRUE.equals(v.getActive()) && (v.getExpiredAt() == null || v.getExpiredAt().isAfter(now)))
                    .collect(Collectors.toList());
            } else if ("hidden".equals(status)) {
                // Lấy voucher bị tắt HOẶC đã hết hạn
                list = list.stream()
                    .filter(v -> !Boolean.TRUE.equals(v.getActive()) || (v.getExpiredAt() != null && v.getExpiredAt().isBefore(now)))
                    .collect(Collectors.toList());
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
            voucher.setMembership(null);
        }

        // 3. XỬ LÝ TỰ ĐỘNG KÍCH HOẠT LẠI KHI GIA HẠN NGÀY
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getExpiredAt() != null) {
            if (voucher.getExpiredAt().isAfter(now)) {
                // Nếu ngày hết hạn mới nằm ở tương lai -> Tự động bật Active để hiện lại
                voucher.setActive(true);
            } else {
                // Nếu ngày ở quá khứ -> Tự động ẩn
                voucher.setActive(false);
            }
        } else {
            // Nếu không chọn ngày, mặc định cho 1 tháng kể từ bây giờ và Active
            voucher.setExpiredAt(now.plusMonths(1));
            voucher.setActive(true);
        }

        voucherRepo.save(voucher);
        return "redirect:/admin/vouchers";
    }

    // 4. ẨN VOUCHER (Ẩn thủ công)
    @GetMapping("/hide/{id}")
    public String hideVoucher(@PathVariable("id") Integer id) {
        voucherRepo.findById(id).ifPresent(v -> {
            v.setActive(false);
            voucherRepo.save(v);
        });
        return "redirect:/admin/vouchers";
    }

    // 5. HIỆN VOUCHER (Mở lại thủ công)
    @GetMapping("/show/{id}")
    public String showVoucher(@PathVariable("id") Integer id) {
        voucherRepo.findById(id).ifPresent(v -> {
            v.setActive(true);
            // Lưu ý: Nếu bấm hiện nhưng ngày vẫn ở quá khứ, logic ở View vẫn sẽ báo là Hết hạn
            voucherRepo.save(v);
        });
        return "redirect:/admin/vouchers";
    }
}