package com.example.demo.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Category;
import com.example.demo.repository.CategoryRepository;

import java.util.List;

@Controller
@RequestMapping("/cata-mana")
public class CataManaController {

    @Autowired
    private CategoryRepository categoryRepo;

    // 🗂️ Đường dẫn thư mục upload (trong static)
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/categories/";

    // 📋 Trang danh sách danh mục
    @GetMapping
    public String showCategories(Model model) {
        List<Category> list = categoryRepo.findAll();
        model.addAttribute("categories", list);
        return "admin/catagoriesMana"; // ⚠️ Trùng đúng với file bạn đang có
    }

    // ➕ Thêm danh mục mới
    @PostMapping("/add")
    public String addCategory(@RequestParam("name") String name,
                              @RequestParam("image") MultipartFile imageFile) {

        if (name == null || name.trim().isEmpty()) {
            return "redirect:/cata-mana?error=emptyName";
        }

        try {
            String fileName = imageFile.getOriginalFilename();
            if (fileName != null && !fileName.isEmpty()) {
                // Tạo thư mục nếu chưa có
                File dir = new File(UPLOAD_DIR);
                if (!dir.exists()) dir.mkdirs();

                // Ghi file ảnh vào thư mục uploads
                Path path = Paths.get(UPLOAD_DIR + fileName);
                Files.copy(imageFile.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                // Tạo mới category
                Category category = new Category();
                category.setName(name);
                category.setImage("/uploads/categories/" + fileName);
                categoryRepo.save(category);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return "redirect:/cata-mana";
    }

    // ✏️ Cập nhật danh mục
    @PostMapping("/update")
    public String updateCategory(@RequestParam("id") Integer id,
                                 @RequestParam("name") String name,
                                 @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {

        Category category = categoryRepo.findById(id).orElse(null);
        if (category == null) {
            return "redirect:/cata-mana?error=notfound";
        }

        category.setName(name);

        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                File dir = new File(UPLOAD_DIR);
                if (!dir.exists()) dir.mkdirs();

                String fileName = imageFile.getOriginalFilename();
                Path path = Paths.get(UPLOAD_DIR + fileName);
                Files.copy(imageFile.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                category.setImage("/uploads/categories/" + fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        categoryRepo.save(category);
        return "redirect:/cata-mana";
    }
}
