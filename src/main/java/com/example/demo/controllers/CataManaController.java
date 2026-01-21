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
	//Mẹ xuân huy rất ngon
    @Autowired
    private CategoryService categoryService;

    // Thư mục lưu trữ
    private static final String UPLOAD_DIR = "uploads/categories/";

    @GetMapping
    public String showCategories(Model model) {
        List<Category> categories = categoryService.findAll();
        model.addAttribute("categories", categories);
        return "admin/catagoriesMana";
    }

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
            if (imagePath != null) {
                category.setImage(imagePath);
            }
        }

        categoryService.save(category);
        return "redirect:/cata-mana?success=add";
    }

    @PostMapping("/update")
    public String updateCategory(@RequestParam("id") Integer id,
                                 @RequestParam("name") String name,
                                 @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {

        Category category = categoryService.findById(id);
        if (category == null) return "redirect:/cata-mana?error=notfound";

        category.setName(name);

        if (imageFile != null && !imageFile.isEmpty()) {
            String imagePath = uploadImage(imageFile);
            if (imagePath != null) {
                category.setImage(imagePath);
            }
        }

        categoryService.save(category);
        return "redirect:/cata-mana?success=update";
    }

    @PostMapping("/delete")
    public String deleteCategory(@RequestParam("id") Integer id) {
        categoryService.delete(id);
        return "redirect:/cata-mana?success=delete";
    }

    private String uploadImage(MultipartFile file) {
        try {
            // 1. Tạo đường dẫn tuyệt đối để tránh lỗi lạc trôi file
            Path root = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }

            // 2. Xử lý tên file và đuôi file
String originalName = file.getOriginalFilename();
            String extension = ".jpg"; // Mặc định
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            
            String fileName = UUID.randomUUID().toString() + extension;
            Path targetPath = root.resolve(fileName);

            // 3. Copy file vào thư mục
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 4. Trả về đường dẫn ảo để hiển thị trên web
            return "/images/categories/" + fileName;

        } catch (IOException e) {
            System.err.println("Lỗi Upload: " + e.getMessage());
            return null;
        }
    }
}