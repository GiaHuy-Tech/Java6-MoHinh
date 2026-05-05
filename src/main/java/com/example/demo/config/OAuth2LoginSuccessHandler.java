package com.example.demo.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private AccountRepository accountRepo;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws ServletException, IOException {

        System.out.println("DEBUG: Đã vào OAuth2LoginSuccessHandler thành công!");

        DefaultOAuth2User oauthUser = (DefaultOAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");

        Account account = accountRepo.findByEmail(email).orElse(null);

        if (account != null) {
            request.getSession().setAttribute("account", account);
        }

        // ÉP CHUYỂN HƯỚNG VỀ TRANG CHỦ, KHÔNG CẦN TÍNH TOÁN REQUEST CŨ
        getRedirectStrategy().sendRedirect(request, response, "/");
        System.out.println("DEBUG: Đã redirect về trang chủ thành công.");
    }
}