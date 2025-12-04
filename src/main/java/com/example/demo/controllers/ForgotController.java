package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;
import com.example.demo.service.MailService;

import java.util.Optional;
import java.util.UUID;

@Controller
public class ForgotController {

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private MailService mailService;

    @GetMapping("/forgot")
    public String forgotForm() {
        return "client/forgot";
    }

    @PostMapping("/forgot")
    public String processForgot(@RequestParam("email") String email, Model model) {
        Optional<Account> accOpt = accountRepo.findByEmail(email);

        if (accOpt.isEmpty()) {
            model.addAttribute("error", "❌ Email không tồn tại trong hệ thống!");
            return "client/forgot";
        }

        Account acc = accOpt.get();

        // ✅ Tạo mật khẩu tạm thời (8 ký tự)
        String tempPass = UUID.randomUUID().toString().substring(0, 8);
        acc.setPassword(tempPass); 
        accountRepo.save(acc);

        // ✅ Gửi email thông báo
        String subject = "Khôi phục mật khẩu - WebShop";
        String body = "Xin chào " + acc.getFullName()
                + ",\n\nMật khẩu mới của bạn là: " + tempPass
                + "\nVui lòng đăng nhập và đổi mật khẩu ngay sau khi vào lại hệ thống."
                + "\n\nTrân trọng,\nĐội ngũ Mom Physic High End Model";

        mailService.send(email, subject, body);

        model.addAttribute("message", "✅ Mật khẩu mới đã được gửi đến email của bạn!");
        return "client/forgot";
    }
}
