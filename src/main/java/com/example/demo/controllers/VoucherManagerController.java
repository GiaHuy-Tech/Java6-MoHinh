package com.example.demo.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    // 1. HIỆN DANH SÁCH + FORM (Mode: Thêm mới hoặc Sửa)
    @GetMapping
    public String listVouchers(Model model) {
        return loadPage(model, new Voucher());
    }

    // 2. CHẾ ĐỘ SỬA: Load lại trang nhưng điền dữ liệu cũ vào form
    @GetMapping("/edit/{id}")
    public String editVoucher(@PathVariable("id") Integer id, Model model) {
        // Tìm voucher theo Integer id
        Voucher voucher = voucherRepo.findById(id).orElse(new Voucher());
        return loadPage(model, voucher);
    }

    // Hàm phụ để load dữ liệu chung cho cả 2 action trên
    private String loadPage(Model model, Voucher voucher) {
        List<Voucher> list = voucherRepo.findAll();
        List<Account> accounts = accountRepo.findAll();

        model.addAttribute("vouchers", list);
        model.addAttribute("accounts", accounts);
        model.addAttribute("voucher", voucher);

        return "admin/voucher-list"; // Trả về file HTML
    }

    // 3. LƯU (THÊM HOẶC SỬA)
    @PostMapping("/save")
    public String saveVoucher(@ModelAttribute("voucher") Voucher voucher,
                              @RequestParam(name = "accountId", required = false) Integer accountId) {

        // 1. Xử lý gán Account (ManyToOne)
        if (accountId != null && accountId > 0) {
            // Nếu chọn user cụ thể
            Account acc = accountRepo.findById(accountId).orElse(null);
            voucher.setAccount(acc);
        } else {
            // Nếu chọn "Tất cả" hoặc không chọn -> Voucher chung
            voucher.setAccount(null);
        }

        // 2. Xử lý logic Active mặc định nếu là thêm mới
        if (voucher.getId() == null) {
            voucher.setActive(true); // Mặc định voucher mới tạo sẽ active
        }

        // 3. (Tùy chọn) Kiểm tra ngày hết hạn logic
        if (voucher.getExpiredAt() == null) {
            // Ví dụ: Mặc định hết hạn sau 1 tháng nếu không nhập
             voucher.setExpiredAt(LocalDateTime.now().plusMonths(1));
        }

        voucherRepo.save(voucher);
        return "redirect:/admin/vouchers";
    }

    // 4. XÓA MỀM (ẨN VOUCHER)
    @GetMapping("/hide/{id}")
    public String hideVoucher(@PathVariable("id") Integer id) {
        Optional<Voucher> v = voucherRepo.findById(id);
        if (v.isPresent()) {
            Voucher voucher = v.get();
            voucher.setActive(false);
            voucherRepo.save(voucher);
        }
        return "redirect:/admin/vouchers";
    }

    // 5. HIỆN LẠI VOUCHER
    @GetMapping("/show/{id}")
    public String showVoucher(@PathVariable("id") Integer id) {
        Optional<Voucher> v = voucherRepo.findById(id);
        if (v.isPresent()) {
            Voucher voucher = v.get();
            voucher.setActive(true);
            voucherRepo.save(voucher);
        }
        return "redirect:/admin/vouchers";
    }

    // 6. (MỞ RỘNG) XÓA CỨNG - Nếu muốn xóa hẳn khỏi database
    @GetMapping("/delete/{id}")
    public String deleteVoucher(@PathVariable("id") Integer id) {
        // Lưu ý: Chỉ xóa được nếu chưa có dữ liệu bên bảng VoucherDetail tham chiếu tới
        try {
            voucherRepo.deleteById(id);
        } catch (Exception e) {
            // Xử lý lỗi ràng buộc khóa ngoại (nếu voucher đã được dùng trong VoucherDetail)
            System.out.println("Không thể xóa voucher đã được sử dụng!");
        }
        return "redirect:/admin/vouchers";
    }
}