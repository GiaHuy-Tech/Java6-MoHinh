package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.demo.model.Account;
import com.example.demo.model.Membership;
import com.example.demo.repository.MembershipRepository;

@Service
public class MembershipService {

    @Autowired
    private MembershipRepository membershipRepo;

    // 1. Cập nhật hạng thành viên tự động dựa trên mức điểm động trong Database
    public void updateMembershipLevel(Account account) {
        BigDecimal spent = account.getTotalSpending();
        if (spent == null) {
            spent = BigDecimal.ZERO;
        }

        // Quy đổi chi tiêu ra điểm (Ví dụ: 10.000 VNĐ = 1 Điểm)
        int currentPoints = spent.divide(new BigDecimal("10000")).intValue();

        // Lấy tất cả hạng mức, sắp xếp theo điểm yêu cầu từ Cao xuống Thấp
        List<Membership> tiers = membershipRepo.findAll(Sort.by(Sort.Direction.DESC, "pointRequired"));

        for (Membership tier : tiers) {
            if (currentPoints >= tier.getPointRequired()) {
                account.setMembership(tier);
                break;
            }
        }
    }

    // 2. Logic kiểm tra Freeship dựa trên Hạng
    public boolean checkFreeShipping(Account account) {
        if (account != null && account.getMembership() != null) {
            return Boolean.TRUE.equals(account.getMembership().getFreeShipping());
        }
        return false;
    }

    // 3. Logic tặng Voucher Sinh Nhật
    public String getBirthdayVoucherCode(LocalDate birthday) {
        if (birthday == null) {
			return null;
		}

        LocalDate today = LocalDate.now();

        // Trùng ngày và tháng sinh thì tặng mã Code
        if (birthday.getMonthValue() == today.getMonthValue() &&
            birthday.getDayOfMonth() == today.getDayOfMonth()) {
            return "HPBD" + today.getYear() + "_20PERCENT";
        }

        return null;
    }
}