package com.example.demo.service;

import java.time.LocalDate; // 1. Thêm import này
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;

import jakarta.servlet.http.HttpSession;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AccountRepository accountRepo;
    private final HttpSession session;

    public CustomOAuth2UserService(AccountRepository accountRepo, HttpSession session) {
        this.accountRepo = accountRepo;
        this.session = session;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");

        Optional<Account> optionalAccount = accountRepo.findByEmail(email);
        Account account;

        if (optionalAccount.isPresent()) {
            account = optionalAccount.get();
        } else {
            // Tạo account mới với giá trị mặc định hợp lệ
            account = new Account();
            account.setEmail(email);
            account.setPassword("google-login"); // mật khẩu tạm thời
            account.setRole(false); // user thường
            account.setActived(true);

            // Lấy tên từ Google, nếu không có thì dùng email
            String name = oAuth2User.getAttribute("name");
            account.setFullName(name != null ? name : email);

            account.setAddress("Chưa cập nhật");
            account.setPhone("0000000000");
            account.setGender(false); // false = nữ

            // --- ĐOẠN SỬA LỖI ---
            // Thay vì dùng Calendar, dùng LocalDate.of
            account.setBirthDay(LocalDate.of(2000, 1, 1));
            // --------------------

            // Lấy avatar từ Google
            String picture = oAuth2User.getAttribute("picture");
            account.setPhoto(picture);

            accountRepo.save(account);
        }

        // Lưu account vào session
        session.setAttribute("account", account);

        // Chuyển Boolean role -> ROLE_USER / ROLE_ADMIN
        String roleName = (account.getRole() != null && account.getRole()) ? "ROLE_ADMIN" : "ROLE_USER";
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleName));

        // Trả về OAuth2User kèm authorities để Spring Security cho phép truy cập route
        return new DefaultOAuth2User(
            authorities,
            oAuth2User.getAttributes(),
            "email" // field dùng làm username
        );
    }
}