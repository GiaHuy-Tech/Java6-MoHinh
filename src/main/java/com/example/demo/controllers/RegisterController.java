package com.example.demo.controllers;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;

import jakarta.validation.Valid;

@Controller
public class RegisterController {

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ================== SHOW FORM ==================
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("account", new Account());
        return "client/register";
    }

    // ================== PROCESS REGISTER ==================
    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute("account") Account account,
            BindingResult result,
            Model model) {

        // 1️⃣ Kiểm tra email trùng
        if (accountRepo.existsByEmail(account.getEmail())) {
            result.rejectValue("email", "error.account", "Email đã tồn tại");
        }

        // 2️⃣ Kiểm tra phone trùng
        if (accountRepo.existsByPhone(account.getPhone())) {
            result.rejectValue("phone", "error.account", "Số điện thoại đã tồn tại");
        }

        if (result.hasErrors()) {
            return "client/register";
        }

        // 3️⃣ Thiết lập mặc định
        account.setActive(true);
        account.setRole(false); // user
        account.setTotalSpending(BigDecimal.ZERO);

        // 4️⃣ Mã hoá password
        account.setPassword(passwordEncoder.encode(account.getPassword()));

        // 5️⃣ Lưu DB
        accountRepo.save(account);

        model.addAttribute("message", "Đăng ký thành công! Vui lòng đăng nhập.");
        return "redirect:/login";
    }
}