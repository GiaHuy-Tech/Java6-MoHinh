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

    // ✅ 1. Hàm gửi mail cơ bản (Dùng cho Quên mật khẩu, v.v...)
    public void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    // ✅ 2. Hàm gửi mail trạng thái (nếu bạn có dùng ở chỗ khác)
    public void sendStatusMail(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        // message.setFrom("..."); // Có thể bỏ qua nếu đã cấu hình trong application.properties
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    // ✅ 3. Hàm gửi mail sinh nhật (HTML đẹp + tính tuổi)
    public void sendBirthdayEmail(String toEmail, String fullName, int age) {
        String subject = "🎉 CHÚC MỪNG SINH NHẬT TỪ MODEL WORLD! 🎉";
        
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