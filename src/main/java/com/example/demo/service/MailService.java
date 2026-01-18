package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    // ... giữ nguyên các hàm send và sendStatusMail cũ ...

    // ✅ CẬP NHẬT: Thêm tham số int age
    public void sendBirthdayEmail(String toEmail, String fullName, int age) {
        String subject = "🎉 CHÚC MỪNG SINH NHẬT TỪ MODEL WORLD! 🎉";
        
        // Nội dung HTML có thêm tuổi
        String content = "<h3>Xin chào " + fullName + ",</h3>"
                + "<p>Chúc mừng bạn đã chính thức bước sang tuổi <strong>" + age + "</strong> rực rỡ!</p>"
                + "<p>Model World chúc bạn tuổi mới thật nhiều niềm vui, sức khỏe dồi dào và gặt hái được nhiều thành công hơn nữa.</p>"
                + "<p>🎁 <strong>Quà tặng đặc biệt:</strong> Nhân dịp sinh nhật lần thứ " + age + ", hãy ghé cửa hàng để nhận ưu đãi dành riêng cho bạn nhé!</p>"
                + "<br>"
                + "<p>Thân ái,<br><strong>Đội ngũ Model World</strong></p>";

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true); 
            
            mailSender.send(message);
            System.out.println("✅ Đã gửi mail chúc mừng sinh nhật " + age + " tuổi cho: " + toEmail);
        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("❌ Lỗi khi gửi mail cho: " + toEmail);
        }
    }
}