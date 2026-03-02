package com.example.demo.controllers;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;

@Controller
public class LoginController {

    @Autowired
    private AccountRepository accountRepo;

    // ================= LOGIN PAGE =================
    @GetMapping("/login")
    public String showLoginForm(HttpServletRequest request, Model model) {

        Account acc = new Account();

        String email = getCookieValue(request, "email");
        String password = getCookieValue(request, "password");

        if (email != null && password != null) {
            acc.setEmail(email);
            acc.setPassword(password);
        }

        boolean remember = (email != null && password != null);

        model.addAttribute("account", acc);
        model.addAttribute("remember", remember);

        return "client/login";
    }

    // ================= PROCESS LOGIN =================
    @PostMapping("/login")
    public String processLogin(
            @Valid @ModelAttribute("account") Account account,
            BindingResult result,
            @RequestParam(value = "remember", required = false) String remember,
            Model model,
            HttpServletResponse response,
            HttpSession session) {

        if (result.hasErrors()) {
            return "client/login";
        }

        Optional<Account> optionalAccount = accountRepo.findByEmail(account.getEmail());

        if (optionalAccount.isEmpty()) {
            model.addAttribute("errorMessage", "Tài khoản không tồn tại!");
            return "client/login";
        }

        Account dbAccount = optionalAccount.get();

        if (!account.getPassword().equals(dbAccount.getPassword())) {
            model.addAttribute("errorMessage", "Mật khẩu không đúng!");
            return "client/login";
        }

        // ================= ĐĂNG NHẬP THÀNH CÔNG =================
        session.setAttribute("user", dbAccount);   // 🔥 QUAN TRỌNG: dùng "user"

        System.out.println("Login thành công với ID: " + dbAccount.getId());

        // ================= REMEMBER ME =================
        if (remember != null) {
            saveCookie(response, "email", account.getEmail(), 7);
            saveCookie(response, "password", account.getPassword(), 7);
        } else {
            clearCookie(response, "email");
            clearCookie(response, "password");
        }

        return "redirect:/home";
    }

    // ================= LOGOUT =================
    @GetMapping("/logout")
    public String logout(HttpServletResponse response, HttpSession session) {

        session.invalidate();

        clearCookie(response, "email");
        clearCookie(response, "password");

        return "redirect:/login";
    }

    // ================= COOKIE METHODS =================
    private void saveCookie(HttpServletResponse response, String name, String value, int days) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(days * 24 * 60 * 60);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    private void clearCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;

        return Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}