package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Category;
import com.example.demo.model.ProductImage;
import com.example.demo.model.Products;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;

@Controller
@RequestMapping("/product-mana")
public class ProductManaController {

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private CategoryRepository categoryRepo;

    // Lưu ảnh vào ./uploads/products
    public static final String UPLOAD_DIRECTORY =
            System.getProperty("user.dir") + "/uploads/products";

    @GetMapping
    public String list(Model model) {

        model.addAttribute("list", productRepo.findAll());
        model.addAttribute("product", new Products());
        model.addAttribute("categories", categoryRepo.findAll());

        return "admin/productMana";
    }

    @PostMapping("/add")
    public String add(
            @ModelAttribute("product") Products product,
            @RequestParam("imageFiles") MultipartFile[] files,
            @RequestParam(defaultValue = "0") int thumbnailIndex,
            Model model
    ) throws IOException {

        long fileCount = Arrays.stream(files)
                .filter(f -> !f.isEmpty())
                .count();

        if (fileCount < 2) {

            model.addAttribute("message", "Vui lòng chọn tối thiểu 2 hình ảnh!");
            model.addAttribute("messageType", "danger");

            model.addAttribute("list", productRepo.findAll());
            model.addAttribute("categories", categoryRepo.findAll());

            return "admin/productMana";
        }

        product.setCreatedDate(new Date());

        Integer qty = product.getQuantity() == null ? 0 : product.getQuantity();
        product.setQuantity(qty);
        product.setAvailable(qty > 0);

        if (product.getWeight() == null || product.getWeight() <= 0) {
            product.setWeight(0.5);
        }

        if (product.getCategory() != null &&
                product.getCategory().getId() != null) {

            Category c = categoryRepo
                    .findById(product.getCategory().getId())
                    .orElse(null);

            product.setCategory(c);
        }

        saveImages(product, files, thumbnailIndex);

        productRepo.save(product);

        return "redirect:/product-mana";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {

        Products p = productRepo.findById(id).orElse(null);

        model.addAttribute("product", p);
        model.addAttribute("list", productRepo.findAll());
        model.addAttribute("categories", categoryRepo.findAll());

        return "admin/productMana";
    }

    @PostMapping("/update")
    public String update(
            @ModelAttribute Products product,
            @RequestParam(required = false) MultipartFile[] imageFiles,
            @RequestParam(defaultValue = "-1") int thumbnailIndex
    ) throws IOException {

        Products old = productRepo.findById(product.getId()).orElse(null);

        if (old == null) {
            return "redirect:/product-mana";
        }

        old.setName(product.getName());
        old.setPrice(product.getPrice());
        old.setDescription(product.getDescription());

        Integer qty = product.getQuantity() == null ? 0 : product.getQuantity();
        old.setQuantity(qty);
        old.setAvailable(qty > 0);

        if (product.getWeight() != null && product.getWeight() > 0) {
            old.setWeight(product.getWeight());
        }

        if (product.getCategory() != null &&
                product.getCategory().getId() != null) {

            Category c = categoryRepo
                    .findById(product.getCategory().getId())
                    .orElse(null);

            old.setCategory(c);
        }

        if (imageFiles != null && imageFiles.length > 0) {

            boolean hasFile = Arrays.stream(imageFiles)
                    .anyMatch(f -> !f.isEmpty());

            if (hasFile) {

                if (thumbnailIndex >= 0 && old.getImages() != null) {
                    old.getImages().forEach(i -> i.setThumbnail(false));
                }

                saveImages(old, imageFiles, thumbnailIndex);
            }
        }

        productRepo.save(old);

        return "redirect:/product-mana";
    }

    private void saveImages(
            Products product,
            MultipartFile[] files,
            int thumbnailIndex
    ) throws IOException {

        Path uploadPath = Paths.get(UPLOAD_DIRECTORY);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        for (int i = 0; i < files.length; i++) {

            MultipartFile f = files[i];

            if (!f.isEmpty()) {

                String fileName =
                        System.currentTimeMillis() + "_" + f.getOriginalFilename();

                Path filePath = uploadPath.resolve(fileName);

                Files.copy(
                        f.getInputStream(),
                        filePath,
                        StandardCopyOption.REPLACE_EXISTING
                );

                ProductImage img = new ProductImage();

                // Đường dẫn hiển thị trên web
                img.setImage("/images/products/" + fileName);

                img.setThumbnail(i == thumbnailIndex);

                img.setProduct(product);

                product.getImages().add(img);
            }
        }
    }
}