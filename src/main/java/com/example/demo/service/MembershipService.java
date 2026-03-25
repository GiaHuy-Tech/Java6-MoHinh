package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import com.example.demo.model.Account;

@Service
public class MembershipService {

    // 1. Cập nhật hạng thành viên dựa trên chi tiêu
    public void updateMembershipLevel(Account account) {
        // BigDecimal không được gán trực tiếp cho long, phải dùng null check và so sánh compareTo
        BigDecimal spent = account.getTotalSpending();
        if (spent == null) {
            spent = BigDecimal.ZERO;
        }

        // Định nghĩa các mốc tiền bằng BigDecimal để so sánh chính xác
        BigDecimal diamondLimit = new BigDecimal("1000000000"); // 1 Tỷ
        BigDecimal goldLimit = new BigDecimal("100000000");    // 100 Triệu
        BigDecimal silverLimit = new BigDecimal("10000000");   // 10 Triệu

        String level;
        if (spent.compareTo(diamondLimit) >= 0) {
            level = "Kim Cương";
        } else if (spent.compareTo(goldLimit) >= 0) {
            level = "Vàng";
        } else if (spent.compareTo(silverLimit) >= 0) {
            level = "Bạc";
        } else {
            level = "Đồng";
        }
        
        // Lưu ý: Đảm bảo class Account có trường String membershipLevel 
        // Hoặc nếu bạn dùng entity Membership thì logic sẽ khác một chút (phải gọi Repo)
        account.setMembershipLevel(level); 
    }

    // 2. Lấy ưu đãi theo hạng (Ví dụ % giảm giá)
    public int getDiscountPercent(String level) {
        if (level == null) return 0;
        
        return switch (level) {
            case "Kim Cương" -> 15; // Giảm 15%
            case "Vàng" -> 10;      // Giảm 10%
            case "Bạc" -> 5;       // Giảm 5%
            default -> 0;          // Đồng không giảm
        };
    }

    // 3. Lấy lời chúc và quà sinh nhật
    public String getBirthdayMessage(LocalDate birthday) {
        if (birthday == null) return null;

        LocalDate today = LocalDate.now();

        // So sánh Tháng và Ngày dùng getMonthValue() và getDayOfMonth()
        if (birthday.getMonthValue() == today.getMonthValue() &&
            birthday.getDayOfMonth() == today.getDayOfMonth()) {

            return "🎂 Chúc mừng sinh nhật! MODEL WORLD tặng bạn mã giảm giá 20%: HPBD" + today.getYear();
        }

        return null; 
    }
}