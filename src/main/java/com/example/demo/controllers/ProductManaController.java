package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.*;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Products;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/product-mana")
public class ProductManaController {

    @Autowired
    ProductRepository productRepo;

    @Autowired
    CategoryRepository categoryRepo;

    // 📌 Hiển thị danh sách
    @GetMapping
    public String list(Model model) {
        model.addAttribute("list", productRepo.findAll());
        model.addAttribute("product", new Products());
        model.addAttribute("categories", categoryRepo.findAll());
        return "admin/productMana";
    }

    // 📌 Thêm sản phẩm
    @PostMapping("/add")
    public String add(
            @Valid @ModelAttribute("product") Products product,
            BindingResult result,
            @RequestParam("imageFile") MultipartFile file,
            Model model) throws IOException {

        if (result.hasErrors()) {
            model.addAttribute("list", productRepo.findAll());
            model.addAttribute("categories", categoryRepo.findAll());
            return "admin/productMana";
        }

        if (!file.isEmpty()) {
            String uploadDir = "src/main/resources/static/images/";
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir, fileName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            product.setImage("/images/" + fileName);
        }

        product.setCreatedDate(new Date());
        productRepo.save(product);
        return "redirect:/product-mana";
    }

    // 📌 Load sản phẩm lên form sửa
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        model.addAttribute("product", productRepo.findById(id).orElse(null));
        model.addAttribute("list", productRepo.findAll());
        model.addAttribute("categories", categoryRepo.findAll());
        return "admin/productMana";
    }

    // 📌 Cập nhật sản phẩm
    @PostMapping("/update")
    public String update(
            @Valid @ModelAttribute("product") Products product,
            BindingResult result,
            @RequestParam("imageFile") MultipartFile file,
            Model model) throws IOException {

        if (result.hasErrors()) {
            model.addAttribute("list", productRepo.findAll());
            model.addAttribute("categories", categoryRepo.findAll());
return "admin/productMana";
        }

        Products old = productRepo.findById(product.getId()).orElse(null);
        if (old != null) {
            product.setCreatedDate(old.getCreatedDate());

            if (!file.isEmpty()) {
                String uploadDir = "src/main/resources/static/images/";
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path path = Paths.get(uploadDir, fileName);
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                product.setImage("/images/" + fileName);
            } else {
                product.setImage(old.getImage());
            }
        }

        productRepo.save(product);
        return "redirect:/product-mana";
    }

    // 📌 Xóa
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        productRepo.deleteById(id);
        return "redirect:/product-mana";
    }
}