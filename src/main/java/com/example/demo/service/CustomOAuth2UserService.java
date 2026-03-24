package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private HttpSession session;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 1. Lấy thông tin từ Google
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        // 2. Kiểm tra account trong DB
        Optional<Account> optionalAccount = accountRepo.findByEmail(email);
        Account account;

        if (optionalAccount.isPresent()) {
            account = optionalAccount.get();
        } else {
            // 3. Tạo account mới nếu chưa tồn tại
            account = new Account();
            account.setEmail(email);
            account.setPassword("google-login"); // Mật khẩu giả định
            account.setRole(false);              // false = ROLE_USER
            account.setActive(true);             
            
            account.setFullName(name != null ? name : email);
            account.setPhone(""); 

            // FIX LỖI TẠI ĐÂY: Vì DB không cho phép NULL, gán mặc định là true (hoặc false)
            // User có thể vào trang cá nhân để sửa lại sau.
            account.setGender(true); 

            // Dùng LocalDate.now() hoặc một ngày mặc định
            account.setBirthDay(LocalDate.now()); 

            account.setAvatar(picture);
            account.setTotalSpending(BigDecimal.ZERO);

            // Lưu xuống Database
            accountRepo.save(account);
        }

        // 4. Lưu account vào session để sử dụng trong giao diện
        session.setAttribute("account", account);

        // 5. Cấp quyền (Role)
        String roleName = (Boolean.TRUE.equals(account.getRole())) ? "ROLE_ADMIN" : "ROLE_USER";
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(roleName));

        return new DefaultOAuth2User(
            authorities,
            oAuth2User.getAttributes(),
            "email"
        );
    }
}