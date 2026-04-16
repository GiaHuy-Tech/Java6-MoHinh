package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Account;
import com.example.demo.model.Membership;
import com.example.demo.repository.MembershipRepository;

@Service
public class MembershipService {

    // Tiêm Repository vào để truy vấn Object Membership từ Database
    @Autowired
    private MembershipRepository membershipRepo;

    // 1. Cập nhật hạng thành viên dựa trên chi tiêu
    public void updateMembershipLevel(Account account) {
        BigDecimal spent = account.getTotalSpending();
        if (spent == null) {
            spent = BigDecimal.ZERO;
        }

        // Định nghĩa các mốc tiền (Khớp với mốc điểm Admin Dashboard: 1 điểm = 10k)
        BigDecimal diamondLimit = new BigDecimal("1000000000"); // 1 Tỷ (100,000 điểm)
        BigDecimal goldLimit = new BigDecimal("50000000");      // 50 Triệu (5,000 điểm)
        BigDecimal silverLimit = new BigDecimal("10000000");    // 10 Triệu (1,000 điểm)

        String levelName;
        if (spent.compareTo(diamondLimit) >= 0) {
            levelName = "Kim Cương";
        } else if (spent.compareTo(goldLimit) >= 0) {
            levelName = "Vàng";
        } else if (spent.compareTo(silverLimit) >= 0) {
            levelName = "Bạc";
        } else {
            levelName = "Đồng";
        }

        // ĐÃ SỬA LỖI DÒNG 38: Tìm Object Membership từ DB và gán vào Account
        Membership membership = membershipRepo.findByName(levelName).orElse(null);
        if (membership != null) {
            account.setMembership(membership);
        }
    }

    // 2. Lấy ưu đãi theo hạng (Ví dụ % giảm giá)
    public int getDiscountPercent(String level) {
        if (level == null) {
			return 0;
		}

        return switch (level) {
            case "Kim Cương" -> 15; // Giảm 15%
            case "Vàng" -> 10;      // Giảm 10%
            case "Bạc" -> 5;        // Giảm 5%
            default -> 0;           // Đồng không giảm
        };
    }

    // 3. Lấy lời chúc và quà sinh nhật
    public String getBirthdayMessage(LocalDate birthday) {
        if (birthday == null) {
			return null;
		}

        LocalDate today = LocalDate.now();

        // So sánh Tháng và Ngày
        if (birthday.getMonthValue() == today.getMonthValue() &&
            birthday.getDayOfMonth() == today.getDayOfMonth()) {

            return "🎂 Chúc mừng sinh nhật! MODEL WORLD tặng bạn mã giảm giá 20%: HPBD" + today.getYear();
        }

        return null;
    }
}