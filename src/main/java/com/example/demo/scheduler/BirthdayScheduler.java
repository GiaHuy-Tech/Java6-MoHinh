package com.example.demo.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.model.Account;
import com.example.demo.model.Voucher;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.VoucherRepository;
import com.example.demo.service.MailService;

@Component
public class BirthdayScheduler {

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private VoucherRepository voucherRepo; // Thêm repo này

    @Autowired
    private MailService emailService;

    // Chạy lúc 07:00 mỗi ngày
    @Scheduled(cron = "0 0 7 * * *")
    public void scanAndSendBirthdayEmails() {
        LocalDate today = LocalDate.now();
        int day = today.getDayOfMonth();
        int month = today.getMonthValue();
        int currentYear = today.getYear();

        System.out.println("🔄 Đang quét sinh nhật ngày: " + day + "/" + month);

        List<Account> birthdayAccounts = accountRepo.findByBirthday(month, day);
        // Lấy các voucher mẫu dành cho sinh nhật
        List<Voucher> birthdayVoucherTemplates = voucherRepo.findByIsBirthdayTrueAndAccountIsNull();

        if (birthdayAccounts.isEmpty()) {
            System.out.println("📅 Hôm nay không có ai sinh nhật.");
        } else {
            for (Account acc : birthdayAccounts) {
                // 1. TỰ ĐỘNG TẶNG VOUCHER SINH NHẬT
                for (Voucher template : birthdayVoucherTemplates) {
                    // Kiểm tra xem user này đã có mã voucher này chưa (tránh tặng trùng nếu chạy lại)
                    if (!voucherRepo.existsByAccount_IdAndCode(acc.getId(), template.getCode())) {
                        Voucher newVoucher = Voucher.builder()
                                .code(template.getCode())
                                .discountPercent(template.getDiscountPercent())
                                .discountAmount(template.getDiscountAmount())
                                .minOrderValue(template.getMinOrderValue())
                                .expiredAt(template.getExpiredAt())
                                .active(true)
                                .isFreeShipping(template.getIsFreeShipping())
                                .isBirthday(true)
                                .account(acc) // Gán cho user
                                .build();
                        voucherRepo.save(newVoucher);
                        System.out.println("🎁 Đã tặng tự động voucher " + template.getCode() + " cho " + acc.getFullName());
                    }
                }

                // 2. GỬI MAIL CHÚC MỪNG
                if (acc.getEmail() != null && !acc.getEmail().isEmpty()) {
                    int birthYear = acc.getBirthDay().getYear();
                    int age = currentYear - birthYear;
                    emailService.sendBirthdayEmail(acc.getEmail(), acc.getFullName(), age);
                }
            }
        }
    }
}