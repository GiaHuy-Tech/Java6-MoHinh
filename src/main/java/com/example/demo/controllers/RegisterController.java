package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // Import thêm cái này

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;

import jakarta.validation.Valid;

@Controller
public class RegisterController {

    @Autowired
    private AccountRepository accountRepo;

    // Hiển thị form đăng ký
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("account", new Account());
        return "client/register";
    }

    // Xử lý khi nhấn nút Đăng ký
    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute("account") Account account,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) { // <--- Thêm RedirectAttributes để truyền tin nhắn khi redirect

        System.out.println("==> Dữ liệu nhận được: " + account);
        System.out.println("==> Ngày sinh: " + account.getBirthDay()); // Kiểm tra xem ngày sinh có vào không

        // 1. Kiểm tra email trùng
        try {
            if (accountRepo.existsByEmail(account.getEmail())) {
                result.rejectValue("email", "error.account", "Email này đã được sử dụng");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Kiểm tra phone trùng
        try {
            if (accountRepo.existsByPhone(account.getPhone())) {
                result.rejectValue("phone", "error.account", "Số điện thoại đã tồn tại");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Nếu có lỗi validate -> quay lại form
        if (result.hasErrors()) {
            return "client/register";
        }

        // 4. Thiết lập giá trị mặc định cho tài khoản mới
        account.setActived(true);
        account.setRole(false); // User thường
        account.setMembershipLevel("Đồng"); // <--- Mặc định là hạng Đồng để sau này tính Voucher

        if (account.getPhoto() == null || account.getPhoto().isEmpty()) {
            account.setPhoto("default-avatar.png"); // Nên đặt tên file ảnh mặc định có thật trong thư mục images
        }

        // 5. Lưu vào Database
        try {
            accountRepo.save(account);
            System.out.println("==> Đăng ký thành công: " + account.getEmail());
        } catch (Exception e) {
            System.out.println("==> Lỗi DB: " + e.getMessage());
            model.addAttribute("error", "Lỗi hệ thống: Không thể tạo tài khoản.");
            return "client/register";
        }

        // 6. Thông báo thành công và chuyển hướng đăng nhập
        // Dùng redirectAttributes để tin nhắn tồn tại sau khi chuyển trang
        redirectAttributes.addFlashAttribute("message", "Đăng ký thành công! Vui lòng đăng nhập.");

        return "redirect:/login";
    }
}