package com.example.demo.controllers;

import java.util.Optional;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private HttpSession session;

    // 🟢 Trang login — tự động lấy cookie (nếu có)
    @GetMapping("/login")
    public String showLoginForm(HttpServletRequest request, Model model) {
        // Lấy cookie email và password nếu tồn tại
        String email = getCookieValue(request, "email");
        String password = getCookieValue(request, "password");

        if (email != null && password != null) {
            model.addAttribute("email", email);
            model.addAttribute("password", password);
            model.addAttribute("remember", true);
        }

        return "client/login";
    }

    // 🟢 Xử lý login
    @PostMapping("/login")
    public String processLogin(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(value = "remember", required = false) String remember,
            Model model,
            HttpServletResponse response) {

        Optional<Account> optionalAccount = accountRepo.findByEmail(email);

        if (optionalAccount.isEmpty()) {
            model.addAttribute("error", "Tài khoản không tồn tại!");
            return "client/login";
        }

        Account account = optionalAccount.get();

        if (!password.equals(account.getPassword())) {
            model.addAttribute("error", "Mật khẩu không đúng!");
            return "client/login";
        }

        // ✅ Đăng nhập thành công
        session.setAttribute("account", account);

        // ✅ Nếu chọn “Ghi nhớ đăng nhập” thì lưu cookie 7 ngày
        if (remember != null) {
            saveCookie(response, "email", email, 7);
            saveCookie(response, "password", password, 7);
        } else {
            // Nếu không tick thì xóa cookie cũ
            clearCookie(response, "email");
            clearCookie(response, "password");
        }

        return "redirect:/";
    }

    // 🟢 Logout — xóa session + cookie
    @GetMapping("/logout")
    public String logout(HttpSession session, HttpServletResponse response) {
        session.invalidate();
        clearCookie(response, "email");
        clearCookie(response, "password");
        return "redirect:/login";
    }


    private void saveCookie(HttpServletResponse response, String name, String value, int days) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(days * 24 * 60 * 60); // thời hạn tính bằng giây
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
