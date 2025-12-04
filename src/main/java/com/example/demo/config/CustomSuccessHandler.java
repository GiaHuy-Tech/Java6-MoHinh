package com.example.demo.config;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.demo.model.Account;
import com.example.demo.service.AccountService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    private final AccountService accountService;

    public CustomSuccessHandler(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        // ✅ Lưu thông tin người dùng đăng nhập vào session
        String email = authentication.getName();
        Optional<Account> accOpt = accountService.findByEmail(email);

        HttpSession session = request.getSession();
        if (accOpt.isPresent()) {
            session.setAttribute("account", accOpt.get());
        }

        // ✅ Chuyển hướng theo vai trò
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            if (authority.getAuthority().equals("ROLE_ADMIN")) {
                response.sendRedirect("/stats");
                return;
            } else if (authority.getAuthority().equals("ROLE_USER")) {
                response.sendRedirect("/");
                return;
            }
        }

        // fallback
        response.sendRedirect("/");
    }
}
