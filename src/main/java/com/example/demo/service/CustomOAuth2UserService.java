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
            // Nếu cần cập nhật thông tin mới nhất từ Google thì set lại ở đây
            // account.setAvatar(picture);
            // accountRepo.save(account);
        } else {
            // 3. Tạo account mới nếu chưa tồn tại
            account = new Account();
            account.setEmail(email);
            account.setPassword("google-login"); // Mật khẩu giả định
            account.setRole(false);              // false = User thường
            account.setActive(true);             // SỬA: actived -> active
            
            account.setFullName(name != null ? name : email);
            account.setPhone(""); // Để trống hoặc chuỗi rỗng
            account.setGender(null); // Không xác định được từ Google cơ bản

            // SỬA: Dùng LocalDate thay vì Calendar
            account.setBirthDay(LocalDate.now()); 

            // SỬA: setPhoto -> setAvatar
            account.setAvatar(picture);

            // Khởi tạo chi tiêu bằng 0
            account.setTotalSpending(BigDecimal.ZERO);

            /* * LƯU Ý: Không setAddress vì trong Model mới, Address là 
             * List<Address> (bảng riêng), không phải String.
             * User sẽ cập nhật địa chỉ sau trong trang cá nhân.
             */

            accountRepo.save(account);
        }

        // 4. Lưu account vào session để sử dụng trong controller/view
        session.setAttribute("account", account);

        // 5. Cấp quyền (Role) cho Spring Security
        // Account.role là Boolean: true = ADMIN, false = USER
        String roleName = (Boolean.TRUE.equals(account.getRole())) ? "ROLE_ADMIN" : "ROLE_USER";
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(roleName));

        // 6. Trả về đối tượng User chuẩn của OAuth2
        return new DefaultOAuth2User(
            authorities,
            oAuth2User.getAttributes(),
            "email" // Key để định danh user (primary key logic của Google user map)
        );
    }
}