package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        System.out.println("DEBUG GOOGLE: Bắt đầu loadUser từ Google...");

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        System.out.println("DEBUG GOOGLE: Thông tin lấy về - Email: " + email + ", Name: " + name);

        Optional<Account> optionalAccount = accountRepo.findByEmail(email);
        Account account;

        if (optionalAccount.isPresent()) {
            System.out.println("DEBUG GOOGLE: Đã tìm thấy tài khoản cũ -> Update thông tin.");
            account = optionalAccount.get();

            if (account.getAvatar() == null || account.getAvatar().startsWith("http")) {
                account.setAvatar(picture);
            }
            if (account.getFullName() == null || account.getFullName().isEmpty()) {
                account.setFullName(name);
            }
            if (account.getPassword() == null || account.getPassword().isEmpty()) {
                account.setPassword(UUID.randomUUID().toString());
            }
        } else {
            System.out.println("DEBUG GOOGLE: Tài khoản chưa tồn tại -> Tạo mới.");
            account = new Account();
            account.setEmail(email);
            account.setFullName(name);
            account.setAvatar(picture);
            account.setPassword(UUID.randomUUID().toString());
            account.setRole(false); // ROLE_USER
            account.setActive(true);
            account.setTotalSpending(BigDecimal.ZERO);
            account.setGender(true);
            account.setBirthDay(LocalDate.now());
            account.setPhone("0000000000");
        }

        Account savedAccount = accountRepo.save(account);
        System.out.println("DEBUG GOOGLE: Tài khoản đã lưu vào DB thành công.");

        // Lưu vào session
        session.setAttribute("account", savedAccount);
        session.setAttribute("user", savedAccount);

        String roleName = (Boolean.TRUE.equals(savedAccount.getRole())) ? "ROLE_ADMIN" : "ROLE_USER";

        System.out.println("DEBUG GOOGLE: Trả về OAuth2User với role: " + roleName);

        return new DefaultOAuth2User(
            Collections.singletonList(new SimpleGrantedAuthority(roleName)),
            oAuth2User.getAttributes(),
            "email"
        );
    }
}