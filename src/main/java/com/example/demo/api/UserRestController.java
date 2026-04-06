package com.example.demo.api;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;

@RestController
@RequestMapping("/api/users")
@CrossOrigin("*")
public class UserRestController {

    @Autowired
    private AccountRepository repo;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestParam("password") String password,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("address") String address,
            @RequestParam("gender") Boolean gender,
            @RequestParam("birthDay") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate birthDay,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            // 1. Kiểm tra trùng Email
            if (repo.existsByEmail(email)) {
                return ResponseEntity.badRequest().body("Email đã tồn tại!");
            }

            // 2. Khởi tạo đối tượng Account
            Account acc = new Account();
            acc.setEmail(email);
            acc.setPassword(password);
            acc.setFullName(fullName);
            acc.setPhone(phone);
            acc.setGender(gender);
            acc.setBirthDay(birthDay); // Đã khớp với tên thuộc tính birthDay
            acc.setRole(false);        // Mặc định là User
            acc.setActive(true);       // ĐÃ SỬA: dùng setActive thay vì setActived
            acc.setTotalSpending(BigDecimal.ZERO); // Khởi tạo chi tiêu bằng 0

            // 3. Xử lý file ảnh
            if (file != null && !file.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                
                // Đường dẫn lưu file (Cố định vào thư mục uploads để tránh mất ảnh khi rebuild)
                String uploadDir = new File("uploads/images/").getAbsolutePath();
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                Path filePath = Paths.get(uploadDir, fileName);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                
                // ĐÃ SỬA: dùng setAvatar thay vì setPhoto
                acc.setAvatar(fileName); 
            } else {
                acc.setAvatar("user.png"); // ĐÃ SỬA: dùng setAvatar thay vì setPhoto
            }

            // 4. Lưu vào Database
            repo.save(acc);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(acc);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống: " + e.getMessage());
        }
    }
}