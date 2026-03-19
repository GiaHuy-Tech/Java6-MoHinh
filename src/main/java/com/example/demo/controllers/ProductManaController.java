package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Date;
import java.util.List; // QUAN TRỌNG: Thêm dòng này
import java.util.ArrayList;

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

    public static final String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/uploads/products";

    @GetMapping
    public String list(
            Model model,
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String sort) {

        List<Products> list;

        // 1. Logic lọc dữ liệu
        if (categoryId != null && keywords != null && !keywords.isEmpty()) {
            list = productRepo.findByCategoryIdAndNameContainingIgnoreCase(categoryId, keywords);
        } else if (categoryId != null) {
            list = productRepo.findByCategoryId(categoryId);
        } else if (keywords != null && !keywords.isEmpty()) {
            list = productRepo.findByNameContainingIgnoreCase(keywords);
        } else {
            list = productRepo.findAll();
        }

        // 2. Logic sắp xếp (Sửa lỗi BigDecimal .compareTo)
        if ("priceAsc".equals(sort)) {
            list.sort((p1, p2) -> {
                if (p1.getPrice() == null || p2.getPrice() == null) return 0;
                return p1.getPrice().compareTo(p2.getPrice());
            });
        } else if ("priceDesc".equals(sort)) {
            list.sort((p1, p2) -> {
                if (p1.getPrice() == null || p2.getPrice() == null) return 0;
                return p2.getPrice().compareTo(p1.getPrice());
            });
        } else {
            // Mặc định ID giảm dần (mới nhất lên đầu)
            list.sort((p1, p2) -> p2.getId().compareTo(p1.getId()));
        }

        model.addAttribute("list", list);
        model.addAttribute("product", new Products());
        model.addAttribute("categories", categoryRepo.findAll());
        
        // Gửi lại các giá trị để giữ trạng thái trên Form lọc
        model.addAttribute("keywords", keywords);
        model.addAttribute("selectedCata", categoryId);
        model.addAttribute("selectedSort", sort);

        return "admin/productMana";
    }

    @PostMapping("/add")
    public String add(
            @ModelAttribute("product") Products product,
            @RequestParam("imageFiles") MultipartFile[] files,
            @RequestParam(defaultValue = "0") int thumbnailIndex,
            Model model
    ) throws IOException {

        long fileCount = Arrays.stream(files).filter(f -> !f.isEmpty()).count();

        if (fileCount < 2) {
            model.addAttribute("message", "Vui lòng chọn tối thiểu 2 hình ảnh!");
            model.addAttribute("messageType", "danger");
            model.addAttribute("list", productRepo.findAll());
            model.addAttribute("categories", categoryRepo.findAll());
            return "admin/productMana";
        }

        product.setCreatedDate(new Date());
        product.setQuantity(product.getQuantity() == null ? 0 : product.getQuantity());
        product.setAvailable(product.getQuantity() > 0);
        if (product.getWeight() == null || product.getWeight() <= 0) product.setWeight(0.5);

        if (product.getCategory() != null && product.getCategory().getId() != null) {
            product.setCategory(categoryRepo.findById(product.getCategory().getId()).orElse(null));
        }

        if (product.getImages() == null) product.setImages(new ArrayList<>());
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
        if (old == null) return "redirect:/product-mana";

        old.setName(product.getName());
        old.setPrice(product.getPrice());
        old.setDescription(product.getDescription());
        old.setQuantity(product.getQuantity() == null ? 0 : product.getQuantity());
        old.setAvailable(old.getQuantity() > 0);
        if (product.getWeight() != null) old.setWeight(product.getWeight());

        if (product.getCategory() != null && product.getCategory().getId() != null) {
            old.setCategory(categoryRepo.findById(product.getCategory().getId()).orElse(null));
        }

        if (imageFiles != null && Arrays.stream(imageFiles).anyMatch(f -> !f.isEmpty())) {
            if (thumbnailIndex >= 0 && old.getImages() != null) {
                old.getImages().forEach(i -> i.setThumbnail(false));
            }
            saveImages(old, imageFiles, thumbnailIndex);
        }

        productRepo.save(old);
        return "redirect:/product-mana";
    }

    private void saveImages(Products product, MultipartFile[] files, int thumbnailIndex) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIRECTORY);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        for (int i = 0; i < files.length; i++) {
            MultipartFile f = files[i];
            if (!f.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + f.getOriginalFilename();
                Files.copy(f.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

                ProductImage img = new ProductImage();
                img.setImage("/images/products/" + fileName);
                img.setThumbnail(i == thumbnailIndex);
                img.setProduct(product);
                product.getImages().add(img);
            }
        }
    }
}