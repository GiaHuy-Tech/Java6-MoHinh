package com.example.demo.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        String encodedEmail = email != null ? URLEncoder.encode(email, StandardCharsets.UTF_8) : "";

        String targetUrl;

        // ✅ Email hoặc mật khẩu trống
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            StringBuilder sb = new StringBuilder("/login?");
            if (email == null || email.isBlank()) {
                sb.append("email_error=").append(URLEncoder.encode("Email không được để trống", StandardCharsets.UTF_8));
            }
            if (password == null || password.isBlank()) {
                if (sb.toString().contains("=")) sb.append("&");
                sb.append("pass_error=").append(URLEncoder.encode("Mật khẩu không được để trống", StandardCharsets.UTF_8));
            }
            targetUrl = sb.toString();
        }

        // ✅ Tài khoản bị khóa
        else if (exception.getCause() instanceof LockedException
                || exception.getMessage().contains("bị khóa")) {
            String lockedMsg = URLEncoder.encode("Tài khoản của bạn đã bị khóa!", StandardCharsets.UTF_8);
            targetUrl = "/login?locked=" + lockedMsg + "&email=" + encodedEmail;
        }


        // ✅ Tài khoản không tồn tại
        else if (exception instanceof UsernameNotFoundException) {
            String notFoundMsg = URLEncoder.encode("Tài khoản không tồn tại!", StandardCharsets.UTF_8);
            targetUrl = "/login?notfound=" + notFoundMsg;
        }

        // ✅ Sai mật khẩu
        else {
            String errorMsg = URLEncoder.encode("Email hoặc mật khẩu không đúng", StandardCharsets.UTF_8);
            targetUrl = "/login?error=" + errorMsg + "&email=" + encodedEmail;
        }

        System.out.println("➡ Redirecting to: " + targetUrl); // Debug xem URL

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
