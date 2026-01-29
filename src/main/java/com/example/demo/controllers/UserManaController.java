package com.example.demo.controllers;

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/user-mana")
public class UserManaController {

    @Autowired
    private AccountRepository repo;

    // Đường dẫn lưu ảnh (Nên cấu hình trong application.properties, đây là cách đơn giản)
    private final String UPLOAD_DIR = "src/main/resources/static/images/";

    // ✅ Hiển thị danh sách user
    @GetMapping
    public String index(Model model) {
        model.addAttribute("accounts", repo.findAll());
        // Thêm object rỗng để bind vào form modal thêm mới/sửa (nếu có dùng chung)
        model.addAttribute("account", new Account()); 
        return "admin/usermana";
    }

    // ✅ Cập nhật thông tin User
    @PostMapping("/update/{id}")
    public String update(
            @PathVariable("id") Integer id, 
            @ModelAttribute("account") Account formAcc, // formAcc hứng dữ liệu từ form HTML
            BindingResult result, // Để bắt lỗi validate nếu cần
            @RequestParam("file") MultipartFile file) {
        
        try {
            // 1. Tìm account cũ trong DB
            Optional<Account> optionalAcc = repo.findById(id);
            if (!optionalAcc.isPresent()) {
                return "redirect:/user-mana?error=NotFound";
            }
            
            Account acc = optionalAcc.get();

            // 2. Xử lý upload ảnh (Nếu người dùng có chọn ảnh mới)
            if (!file.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename(); // Thêm timestamp để tránh trùng tên
                Path uploadPath = Paths.get(UPLOAD_DIR);
                
                // Tạo thư mục nếu chưa tồn tại
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                
                // Lưu đường dẫn vào DB
                acc.setPhoto("/images/" + fileName);
            }

            // 3. Cập nhật các thông tin từ Form vào Entity
            // Không cập nhật Password tại đây để bảo mật (trừ khi có tính năng reset pass riêng)
            acc.setFullName(formAcc.getFullName());
            acc.setEmail(formAcc.getEmail());
            acc.setPhone(formAcc.getPhone());
            acc.setAddress(formAcc.getAddress()); // Cập nhật địa chỉ chính của Account
            acc.setGender(formAcc.getGender());   // Cập nhật giới tính
            acc.setBirthDay(formAcc.getBirthDay()); // Cập nhật ngày sinh (LocalDate)
            acc.setRole(formAcc.getRole());       // Admin có thể cấp quyền Admin cho người khác

            // 4. Lưu xuống DB
            repo.save(acc);
            
            return "redirect:/user-mana?success=Updated";
            
        } catch (IOException e) {
            e.printStackTrace();
            return "redirect:/user-mana?error=UploadFailed";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/user-mana?error=UpdateFailed";
        }
    }

    // ✅ Khóa user (Logic: set Actived = false)
    @GetMapping("/lock/{id}")
    public String lock(@PathVariable("id") Integer id) {
        Account acc = repo.findById(id).orElse(null);
        // Kiểm tra null và đảm bảo không khóa chính Admin (nếu Role = true là Admin)
        if (acc != null && (acc.getRole() == null || !acc.getRole())) { 
            acc.setActived(false);
            repo.save(acc);
        }
        return "redirect:/user-mana";
    }

    // ✅ Mở khóa user (Logic: set Actived = true)
    @GetMapping("/unlock/{id}")
    public String unlock(@PathVariable("id") Integer id) {
        Account acc = repo.findById(id).orElse(null);
        if (acc != null) {
            acc.setActived(true);
            repo.save(acc);
        }
        return "redirect:/user-mana";
    }
    
    // ✅ (Tùy chọn) Xóa user - Cần cẩn thận vì liên quan khóa ngoại bảng Address/Order
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") Integer id) {
        // Cần xử lý ràng buộc khóa ngoại trước khi xóa (Address, Orders...)
        // Ở đây tạm thời để try-catch
        try {
            repo.deleteById(id);
        } catch (Exception e) {
            return "redirect:/user-mana?error=CannotDelete";
        }
        return "redirect:/user-mana";
    }
}