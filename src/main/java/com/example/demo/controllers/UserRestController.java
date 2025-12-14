package com.example.demo.controllers;

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;

@RestController
@RequestMapping("/api/users")
@CrossOrigin("*")
public class UserRestController {

    @Autowired
    private AccountRepository repo;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            // ❌ Đã xóa username vì Model không có
            @RequestParam("password") String password,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("address") String address,
            
            // ✅ Thêm 2 trường bắt buộc này (Model yêu cầu @NotNull)
            @RequestParam("gender") Boolean gender,
            @RequestParam("birthDay") @DateTimeFormat(pattern = "yyyy-MM-dd") Date birthDay,

            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            // 1. Kiểm tra trùng Email (Thay cho username)
            if (repo.existsByEmail(email)) {
                return ResponseEntity.badRequest().body("Email đã tồn tại!");
            }

            Account acc = new Account();
            // acc.setUsername(username); // ❌ Xóa dòng này vì Model không có username
            
            acc.setEmail(email); 
            acc.setPassword(password);
            acc.setFullName(fullName);
            acc.setPhone(phone);
            acc.setAddress(address);
            acc.setGender(gender);     // ✅ Phải set giới tính
            acc.setBirthDay(birthDay); // ✅ Phải set ngày sinh
            
            acc.setRole(false);
            acc.setActived(true);

            // 2. Xử lý file ảnh
            if (file != null && !file.isEmpty()) {
                String fileName = file.getOriginalFilename();
                // Lưu ý: Đường dẫn upload nên để cấu hình động hoặc tuyệt đối khi deploy
                String uploadDir = new File("src/main/resources/static/images/").getAbsolutePath();
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs(); // Tạo thư mục nếu chưa có

                Path filePath = Paths.get(uploadDir, fileName);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                acc.setPhoto("/images/" + fileName);
            } else {
                acc.setPhoto("user.png");
            }

            repo.save(acc);
            return ResponseEntity.status(HttpStatus.CREATED).body(acc);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi: " + e.getMessage());
        }
    }
}