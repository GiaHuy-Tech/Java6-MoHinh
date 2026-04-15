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
        // 1. Lấy thông tin thô từ Google trả về
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        // 2. TÌM TÀI KHOẢN TRONG DB (VD: ad@gmail.com)
        Optional<Account> optionalAccount = accountRepo.findByEmail(email);
        Account account;

        if (optionalAccount.isPresent()) {
            // ĐÃ CÓ SẴN: Lấy thông tin tài khoản cũ ra
            account = optionalAccount.get();
            
            // Cập nhật lại Avatar và Tên từ Google cho đồng bộ (Không bắt buộc, nhưng nên làm)
            if (account.getAvatar() == null || account.getAvatar().startsWith("http")) {
                account.setAvatar(picture);
            }
            if (account.getFullName() == null || account.getFullName().isEmpty()) {
                account.setFullName(name);
            }
            
            // BẢO HIỂM MẬT KHẨU: Nếu tài khoản cũ chưa có mk (lỗi hiếm), set mk ngẫu nhiên để Spring ko báo lỗi
            if (account.getPassword() == null || account.getPassword().isEmpty()) {
                account.setPassword(UUID.randomUUID().toString());
            }
            
            System.out.println("===> Đã tìm thấy tài khoản có sẵn: " + email);
        } else {
            // CHƯA CÓ: Tạo mới hoàn toàn
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
            
            System.out.println("===> Đang tạo tài khoản mới từ Google: " + email);
        }

        // 3. LƯU LẠI VÀO DATABASE
        Account savedAccount = accountRepo.save(account);

        // 4. LƯU VÀO SESSION (Để giao diện Thymeleaf gọi được ${session.account})
        session.setAttribute("account", savedAccount);
        session.setAttribute("user", savedAccount);

        // 5. Trả về cho Spring Security với quyền (Role) lấy từ DB
        String roleName = (Boolean.TRUE.equals(savedAccount.getRole())) ? "ROLE_ADMIN" : "ROLE_USER";
        
        return new DefaultOAuth2User(
            Collections.singletonList(new SimpleGrantedAuthority(roleName)),
            oAuth2User.getAttributes(),
            "email"
        );
    }
}