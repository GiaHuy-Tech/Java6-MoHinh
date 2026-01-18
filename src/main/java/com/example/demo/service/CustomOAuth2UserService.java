package com.example.demo.service;

import java.util.Date;
import java.util.Optional;
import java.util.Calendar;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
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
            account.setFullName(email);
            account.setAddress("Chưa cập nhật");
            account.setPhone("0000000000");
            account.setGender(false); // false = nữ
            Calendar cal = Calendar.getInstance();
            cal.set(2000, Calendar.JANUARY, 1);
            account.setBirthDay(cal.getTime());
            account.setPhoto(null);

            accountRepo.save(account);
        }

        // Lưu account vào session
        session.setAttribute("account", account);

        // Chuyển Boolean role -> ROLE_USER / ROLE_ADMIN
        String roleName = account.getRole() != null && account.getRole() ? "ROLE_ADMIN" : "ROLE_USER";
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleName));

        // Trả về OAuth2User kèm authorities để Spring Security cho phép truy cập route
        return new DefaultOAuth2User(
            authorities,
            oAuth2User.getAttributes(),
            "email" // field dùng làm username
        );
    }
}
