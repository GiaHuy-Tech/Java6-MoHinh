package com.example.demo.service;

import com.example.demo.model.Account;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class MembershipService {

    // 1. Cập nhật hạng thành viên dựa trên chi tiêu
    public void updateMembershipLevel(Account account) {
        long spent = account.getTotalSpending() == null ? 0 : account.getTotalSpending();

        if (spent >= 20000000) {
            account.setMembershipLevel("Kim Cương");
        } else if (spent >= 10000000) {
            account.setMembershipLevel("Vàng");
        } else if (spent >= 5000000) {
            account.setMembershipLevel("Bạc");
        } else {
            account.setMembershipLevel("Đồng");
        }
    }

    // 2. Lấy ưu đãi theo hạng (Ví dụ % giảm giá)
    public int getDiscountPercent(String level) {
        if (level == null) return 0;
        switch (level) {
            case "Kim Cương": return 15; // Giảm 15%
            case "Vàng":      return 10; // Giảm 10%
            case "Bạc":       return 5;  // Giảm 5%
            default:          return 0;  // Đồng không giảm
        }
    }

    // 3. Lấy lời chúc và quà sinh nhật
    // Đã sửa: Tham số đầu vào là LocalDate thì dùng trực tiếp, không cần convert
    public String getBirthdayMessage(LocalDate birthday) {
        if (birthday == null) return null;

        LocalDate today = LocalDate.now();

        // So sánh Tháng và Ngày (không so sánh Năm)
        if (birthday.getMonth() == today.getMonth() && 
            birthday.getDayOfMonth() == today.getDayOfMonth()) {
            
            return "🎂 Chúc mừng sinh nhật! Hệ thống tặng bạn mã giảm giá 20%: HPBD2026";
        }
        
        return null; // Không phải sinh nhật
    }
}