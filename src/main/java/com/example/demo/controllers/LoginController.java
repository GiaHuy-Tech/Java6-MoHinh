package com.example.demo.controllers;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class LoginController {

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private HttpSession session;

    // Trang login — tự động lấy cookie (nếu có)
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

    // Xử lý login
    @PostMapping("/login")
    public String processLogin(
             @Valid @ModelAttribute("account") Account account, 
             BindingResult result,
             @RequestParam(value = "remember", required = false) String remember, 
             Model model,
             HttpServletResponse response) {

        // Lưu ý: Nếu trong Account entity bạn không gắn @NotBlank, @Size... 
        // thì result.hasErrors() sẽ không bắt được lỗi validation.
        if (result.hasErrors()) {
            return "client/login";
        }

        Optional<Account> optionalAccount = accountRepo.findByEmail(account.getEmail());

        if (optionalAccount.isEmpty()) {
            model.addAttribute("errorMessage", "Tài khoản không tồn tại!");
            return "client/login";
        }

        Account dbAccount = optionalAccount.get();

        // Kiểm tra mật khẩu
        if (!account.getPassword().equals(dbAccount.getPassword())) {
            model.addAttribute("errorMessage", "Mật khẩu không đúng!");
            return "client/login";
        }

        // BỔ SUNG: Kiểm tra trạng thái hoạt động (Active)
        // Trong Model: private Boolean active = true;
        if (Boolean.FALSE.equals(dbAccount.getActive())) {
            model.addAttribute("errorMessage", "Tài khoản đã bị khóa!");
            return "client/login";
        }

        // Đăng nhập thành công
        session.setAttribute("account", dbAccount);

        // Xử lý Cookie
        if (remember != null) {
            saveCookie(response, "email", account.getEmail(), 7);
            saveCookie(response, "password", account.getPassword(), 7);
        } else {
            clearCookie(response, "email");
            clearCookie(response, "password");
        }

        // Điều hướng sau khi đăng nhập thành công
        // Nếu là Admin thì vào trang admin, User thì về trang chủ
        if (Boolean.TRUE.equals(dbAccount.getRole())) {
             return "redirect:/admin/dashboard"; // Ví dụ đường dẫn admin
        }
        return "redirect:/";
    }

    // Logout
    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        session.invalidate();
        clearCookie(response, "email");
        clearCookie(response, "password");
        return "redirect:/login";
    }

    // --- Utility Methods ---

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
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}