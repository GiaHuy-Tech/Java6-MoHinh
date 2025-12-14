package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Category;
import com.example.demo.service.CategoryService;

@Controller
@RequestMapping("/cata-mana")
public class CataManaController {

    @Autowired
    private CategoryService categoryService;

    // ✅ ĐÚNG THEO StaticResourceConfig
    private static final String UPLOAD_DIR = "uploads/categories/";

    // =======================
    // 📋 Danh sách danh mục
    // =======================
    @GetMapping
    public String showCategories(Model model) {
        List<Category> categories = categoryService.findAll();
        model.addAttribute("categories", categories);
        return "admin/catagoriesMana";
    }

    // =======================
    // ➕ Thêm danh mục
    // =======================
    @PostMapping("/add")
    public String addCategory(@RequestParam("name") String name,
                              @RequestParam("image") MultipartFile imageFile) {

        if (name == null || name.trim().isEmpty()) {
            return "redirect:/cata-mana?error=emptyName";
        }

        Category category = new Category();
        category.setName(name);

        if (imageFile != null && !imageFile.isEmpty()) {
            String imagePath = uploadImage(imageFile);
            category.setImage(imagePath);
        }

        categoryService.save(category);
        return "redirect:/cata-mana";
    }

    // =======================
    // ✏️ Cập nhật danh mục
    // =======================
    @PostMapping("/update")
    public String updateCategory(@RequestParam("id") Integer id,
                                 @RequestParam("name") String name,
                                 @RequestParam(value = "imageFile", required = false)
                                 MultipartFile imageFile) {

        Category category = categoryService.findById(id);
        if (category == null) {
            return "redirect:/cata-mana?error=notfound";
        }

        category.setName(name);

        if (imageFile != null && !imageFile.isEmpty()) {
            String imagePath = uploadImage(imageFile);
            category.setImage(imagePath);
        }

        categoryService.save(category);
        return "redirect:/cata-mana";
    }

    // =======================
    // 🗑️ Xóa danh mục
    // =======================
    @PostMapping("/delete")
    public String deleteCategory(@RequestParam("id") Integer id) {
        categoryService.delete(id);
        return "redirect:/cata-mana";
    }

    // =======================
    // 🔧 Upload ảnh (CHUẨN)
    // =======================
    private String uploadImage(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);

            // Tạo thư mục nếu chưa có
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String original = file.getOriginalFilename();
            String ext = original.substring(original.lastIndexOf("."));
            String fileName = UUID.randomUUID() + ext;

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath,
                    StandardCopyOption.REPLACE_EXISTING);

            // ✅ ĐƯỜNG TRẢ VỀ DÙNG CHO IMG SRC
            return "/images/categories/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("Upload image failed", e);
        }
    }
}
