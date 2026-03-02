package com.example.demo.controllers;

import java.math.BigDecimal;

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

    private final AccountRepository accountRepo;
    private final PasswordEncoder passwordEncoder;

    public RegisterController(AccountRepository accountRepo,
                              PasswordEncoder passwordEncoder) {
        this.accountRepo = accountRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("account", new Account());
        return "client/register";
    }

    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute("account") Account account,
            BindingResult result,
            Model model) {

        if (accountRepo.existsByEmail(account.getEmail())) {
            result.rejectValue("email", "error.account", "Email đã tồn tại");
        }

        if (result.hasErrors()) {
            return "client/register";
        }

        account.setActive(true);
        account.setRole(false);
        account.setTotalSpending(BigDecimal.ZERO);

        // ✅ BCrypt encode
        account.setPassword(passwordEncoder.encode(account.getPassword()));

        accountRepo.save(account);

        return "redirect:/login?registered";
    }
}