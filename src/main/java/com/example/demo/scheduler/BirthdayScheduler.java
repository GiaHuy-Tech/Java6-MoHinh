package com.example.demo.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;
import com.example.demo.service.MailService;

@Component
public class BirthdayScheduler {

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private MailService emailService;

    // Chạy lúc 12:15 mỗi ngày
    @Scheduled(cron = "0 0 7 * * *") 
    public void scanAndSendBirthdayEmails() {
        LocalDate today = LocalDate.now();
        int day = today.getDayOfMonth();
        int month = today.getMonthValue();
        int currentYear = today.getYear(); // Lấy năm hiện tại

        System.out.println("🔄 Đang quét sinh nhật ngày: " + day + "/" + month);

        List<Account> birthdayAccounts = accountRepo.findByBirthday(month, day);

        if (birthdayAccounts.isEmpty()) {
            System.out.println("📅 Hôm nay không có ai sinh nhật.");
        } else {
            for (Account acc : birthdayAccounts) {
                if (acc.getEmail() != null && !acc.getEmail().isEmpty()) {
                    
                    // ✅ TÍNH TUỔI: Năm nay - Năm sinh
                    // Lưu ý: Đảm bảo acc.getBirthday() trả về LocalDate. 
                    // Nếu nó là sql.Date thì dùng: acc.getBirthday().toLocalDate().getYear()
                    int birthYear = acc.getBirthday().getYear(); 
                    int age = currentYear - birthYear;

                    // Gọi hàm gửi mail mới với số tuổi
                    emailService.sendBirthdayEmail(acc.getEmail(), acc.getFullName(), age);
                }
            }
        }
    }
}