package com.example.demo.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired
    private AccountRepository accountRepo;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws ServletException, IOException {

        // 1. Lấy thông tin user vừa đăng nhập Google thành công
        DefaultOAuth2User oauthUser = (DefaultOAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");

        // 2. Tìm trong DB (chắc chắn có vì CustomOAuth2UserService đã lưu trước đó)
        Account account = accountRepo.findByEmail(email).orElse(null);

        // 3. LƯU VÀO SESSION AN TOÀN TẠI ĐÂY
        if (account != null) {
            request.getSession().setAttribute("account", account);
        }

        // 4. Chuyển hướng về trang chủ (hoặc trang trước khi bấm đăng nhập)
        this.setAlwaysUseDefaultTargetUrl(true);
        this.setDefaultTargetUrl("/"); // Trả về trang chủ sau khi login
        super.onAuthenticationSuccess(request, response, authentication);
    }
}