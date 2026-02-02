package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.ProductImage;
import com.example.demo.model.Products;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;

@Controller
@RequestMapping("/product-mana")
public class ProductManaController {

    @Autowired ProductRepository productRepo;
    @Autowired CategoryRepository categoryRepo;

    // --- CẤU HÌNH ĐƯỜNG DẪN LƯU FILE ---
    // Lưu vào thư mục "uploads/products" để khớp với Config: "file:uploads/products/"
    public static String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/uploads/products";

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

        // 1. Kiểm tra và tạo thư mục nếu chưa tồn tại
        Path uploadPath = Paths.get(UPLOAD_DIRECTORY);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 2. Duyệt qua các file upload
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                MultipartFile f = files[i];
                if (!f.isEmpty()) {
                    // Tạo tên file duy nhất
                    String fileName = System.currentTimeMillis() + "_" + f.getOriginalFilename();

                    // Lưu file vật lý vào ổ cứng (Folder: uploads/products)
                    Path filePath = uploadPath.resolve(fileName);
                    Files.copy(f.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                    // Tạo đối tượng ảnh
                    ProductImage img = new ProductImage();
                    
                    // --- QUAN TRỌNG: URL phải khớp với addResourceHandler ---
                    // Config là: /images/products/** => URL phải là: /images/products/ten-file
                    img.setImage("/images/products/" + fileName); 
                    
                    img.setThumbnail(i == thumbnailIndex);
                    product.addImage(img);

                    // Set ảnh đại diện chính cho Product
                    if (i == thumbnailIndex) {
                        product.setImage(img.getImage());
                    }
                }
            }
        }

        productRepo.save(product);
        return "redirect:/product-mana";
    }

    // ================= EDIT =================
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        Products p = productRepo.findById(id).orElse(null);
        model.addAttribute("product", p);
        model.addAttribute("list", productRepo.findAll());
        model.addAttribute("categories", categoryRepo.findAll());
        return "admin/productMana";
    }

    // ================= UPDATE =================
    @PostMapping("/update")
    public String update(
            @ModelAttribute Products product,
            @RequestParam("imageFiles") MultipartFile[] files,
            @RequestParam(defaultValue = "-1") int thumbnailIndex
    ) throws IOException {

        Products old = productRepo.findById(product.getId()).orElse(null);
        if (old == null) {
            return "redirect:/product-mana";
        }

        // Update thông tin cơ bản
        old.setName(product.getName());
        old.setPrice(product.getPrice());
        old.setAvailable(product.isAvailable());
        old.setCategory(product.getCategory());
        old.setDescription(product.getDescription());

        // Kiểm tra thư mục lưu trữ
        Path uploadPath = Paths.get(UPLOAD_DIRECTORY);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Xử lý upload ảnh mới (nếu có)
        boolean hasNewUpload = false;
        if (files != null && files.length > 0 && !files[0].isEmpty()) {
            hasNewUpload = true;
            
            // Nếu muốn chọn ảnh mới làm thumbnail, reset các thumbnail cũ
            if (thumbnailIndex >= 0 && old.getImages() != null) {
                 old.getImages().forEach(i -> i.setThumbnail(false));
            }

            for (int i = 0; i < files.length; i++) {
                MultipartFile f = files[i];
                if (!f.isEmpty()) {
                    String fileName = System.currentTimeMillis() + "_" + f.getOriginalFilename();
                    Path filePath = uploadPath.resolve(fileName);
                    Files.copy(f.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                    ProductImage img = new ProductImage();
                    
                    // --- SỬA URL CHO KHỚP CONFIG ---
                    img.setImage("/images/products/" + fileName);
                    
                    // Nếu người dùng chọn ảnh này làm thumbnail
                    boolean isThumb = (i == thumbnailIndex);
                    img.setThumbnail(isThumb);

                    old.addImage(img);

                    // Cập nhật ảnh chính của Product
                    if (isThumb) {
                        old.setImage(img.getImage());
                    }
                }
            }
        }
        
        // Nếu không upload ảnh mới, giữ nguyên ảnh cũ và các thông tin khác
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