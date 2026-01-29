package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.*;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.ProductImage;
import com.example.demo.model.Products;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.repository.ProductRepository;

@Controller
@RequestMapping("/product-mana")
public class ProductManaController {

    @Autowired ProductRepository productRepo;
    @Autowired CategoryRepository categoryRepo;
    @Autowired ProductImageRepository imageRepo;

    // ================= LIST =================
    @GetMapping
    public String list(Model model) {
        model.addAttribute("list", productRepo.findAll());
        model.addAttribute("product", new Products());
        model.addAttribute("categories", categoryRepo.findAll());
        return "admin/productMana";
    }

    // ================= ADD =================
    @PostMapping("/add")
    public String add(
            @ModelAttribute Products product,
            @RequestParam("imageFiles") MultipartFile[] files,
            @RequestParam(defaultValue = "0") int thumbnailIndex
    ) throws IOException {

        product.setCreatedDate(new Date());
        productRepo.save(product);

        String uploadDir = "src/main/resources/static/images/";
        Files.createDirectories(Paths.get(uploadDir));

        for (int i = 0; i < files.length; i++) {
            MultipartFile f = files[i];
            if (!f.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + f.getOriginalFilename();
                Path path = Paths.get(uploadDir, fileName);
                Files.copy(f.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                ProductImage img = new ProductImage();
                img.setImage("/images/" + fileName);
                img.setThumbnail(i == thumbnailIndex);

                product.addImage(img);

                if (i == thumbnailIndex) {
                    product.setImage("/images/" + fileName);
                }
            }
        }

        productRepo.save(product);
        return "redirect:/product-mana";
    }

    // ================= EDIT =================
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        Products p = productRepo.findById(id).orElse(new Products());
        model.addAttribute("product", p);
        model.addAttribute("list", productRepo.findAll());
        model.addAttribute("categories", categoryRepo.findAll());
        return "admin/productMana";
    }

    // ================= UPDATE (KHÔNG LỖI ORPHAN) =================
    @PostMapping("/update")
    public String update(
            @ModelAttribute Products product,
            @RequestParam("imageFiles") MultipartFile[] files,
            @RequestParam(defaultValue = "0") int thumbnailIndex
    ) throws IOException {

        Products old = productRepo.findById(product.getId()).orElse(null);
        if (old == null) return "redirect:/product-mana";

        // update thông tin cơ bản
        old.setName(product.getName());
        old.setPrice(product.getPrice());
        old.setAvailable(product.isAvailable());
        old.setCategory(product.getCategory());
        old.setDescription(product.getDescription());

        String uploadDir = "src/main/resources/static/images/";
        Files.createDirectories(Paths.get(uploadDir));

        // Nếu có upload ảnh mới thì thêm ảnh (KHÔNG XÓA ẢNH CŨ)
        if (files != null && files.length > 0 && !files[0].isEmpty()) {

            for (int i = 0; i < files.length; i++) {
                MultipartFile f = files[i];
                if (!f.isEmpty()) {
                    String fileName = System.currentTimeMillis() + "_" + f.getOriginalFilename();
                    Path path = Paths.get(uploadDir, fileName);
                    Files.copy(f.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                    ProductImage img = new ProductImage();
                    img.setImage("/images/" + fileName);
                    img.setThumbnail(i == thumbnailIndex);

                    old.addImage(img);

                    if (i == thumbnailIndex) {
                        old.setImage("/images/" + fileName);
                    }
                }
            }
        }

        productRepo.save(old);
        return "redirect:/product-mana";
    }

    // ================= DELETE =================
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        productRepo.deleteById(id);
        return "redirect:/product-mana";
    }
}
