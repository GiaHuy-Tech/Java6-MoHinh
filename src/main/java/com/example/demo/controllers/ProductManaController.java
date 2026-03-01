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

    public static String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/uploads/products";

    // ================= LIST =================
    @GetMapping
    public String list(Model model) {
        model.addAttribute("list", productRepo.findAll());
        model.addAttribute("product", new Products());
        model.addAttribute("categories", categoryRepo.findAll());
        return "admin/productMana";
    }

    // ================= ADD (ĐÃ CHỈNH SỬA) =================
    @PostMapping("/add")
    public String add(
            @ModelAttribute("product") Products product, // Đặt tên attribute để giữ dữ liệu khi lỗi
            @RequestParam("imageFiles") MultipartFile[] files,
            @RequestParam(defaultValue = "0") int thumbnailIndex,
            Model model // Thêm Model để đẩy thông báo lỗi
    ) throws IOException {

        // 1. KIỂM TRA SỐ LƯỢNG ẢNH (TỐI THIỂU 2)
        long fileCount = Arrays.stream(files).filter(f -> !f.isEmpty()).count();

        if (fileCount < 2) {
            model.addAttribute("message", "Vui lòng chọn tối thiểu 2 hình ảnh cho sản phẩm!");
            model.addAttribute("messageType", "danger"); // Để CSS màu đỏ (nếu dùng Bootstrap)
            
            // Load lại dữ liệu cần thiết cho trang view
            model.addAttribute("list", productRepo.findAll());
            model.addAttribute("categories", categoryRepo.findAll());
            return "admin/productMana"; // Trả về trang cũ thay vì redirect
        }

        // 2. LOGIC TẠO SẢN PHẨM
        product.setCreatedDate(new Date());

        // Fix Quantity
        Integer qty = product.getQuantity() == null ? 0 : product.getQuantity();
        product.setQuantity(qty);

        // Auto Available
        product.setAvailable(qty > 0);

        // Default Weight
        if (product.getWeight() == null || product.getWeight() <= 0) {
            product.setWeight(0.5);
        }

        // Set Category
        if (product.getCategory() != null && product.getCategory().getId() != null) {
            Category c = categoryRepo.findById(product.getCategory().getId()).orElse(null);
            product.setCategory(c);
        }

        // 3. LƯU ẢNH
        saveImages(product, files, thumbnailIndex);

        // 4. LƯU PRODUCT (Cascade sẽ tự lưu ảnh sang bảng products_image)
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
            @RequestParam(required = false) MultipartFile[] imageFiles,
            @RequestParam(defaultValue = "-1") int thumbnailIndex
    ) throws IOException {

        Products old = productRepo.findById(product.getId()).orElse(null);
        if (old == null) return "redirect:/product-mana";

        // Cập nhật thông tin cơ bản
        old.setName(product.getName());
        old.setPrice(product.getPrice());
        old.setDescription(product.getDescription());
        
        Integer qty = product.getQuantity() == null ? 0 : product.getQuantity();
        old.setQuantity(qty);
        old.setAvailable(qty > 0);

        if (product.getWeight() != null && product.getWeight() > 0) {
            old.setWeight(product.getWeight());
        }

        if (product.getCategory() != null && product.getCategory().getId() != null) {
            Category c = categoryRepo.findById(product.getCategory().getId()).orElse(null);
            old.setCategory(c);
        }

        // Xử lý ảnh khi update (Nếu user có chọn ảnh mới)
        if (imageFiles != null && imageFiles.length > 0 && !imageFiles[0].isEmpty()) {
            
            // Nếu muốn update là thay thế toàn bộ ảnh cũ:
            // old.getImages().clear(); 
            
            // Hoặc chỉ reset thumbnail cũ
            if (thumbnailIndex >= 0 && old.getImages() != null) {
                old.getImages().forEach(i -> i.setThumbnail(false));
            }

            saveImages(old, imageFiles, thumbnailIndex);
        }

        productRepo.save(old);
        return "redirect:/product-mana";
    }

    // ================= HANDLE IMAGE (ĐÃ SỬA) =================
    private void saveImages(Products product,
                            MultipartFile[] files,
                            int thumbnailIndex) throws IOException {

        Path uploadPath = Paths.get(UPLOAD_DIRECTORY);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Duyệt qua file, dùng biến đếm riêng j để xác định thumbnail chính xác
        // vì file[i] có thể bị rỗng
        for (int i = 0; i < files.length; i++) {
            MultipartFile f = files[i];
            if (!f.isEmpty()) {
                
                String fileName = System.currentTimeMillis() + "_" + f.getOriginalFilename();
                
                // Copy file vào thư mục
                try (var inputStream = f.getInputStream()) {
                    Files.copy(inputStream, uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                }

                // Tạo đối tượng ProductImage
                ProductImage img = new ProductImage();
                img.setImage("/uploads/products/" + fileName); // Đường dẫn lưu trong DB
                
                // Logic thumbnail: Nếu index file hiện tại khớp với lựa chọn của user
                img.setThumbnail(i == thumbnailIndex);

                // Thêm vào list images của Product (Dùng helper method trong Model Products)
                product.addImage(img);
                
                // LƯU Ý: Đã xóa dòng product.setImage(...) vì Entity Products không có cột này
            }
        }
    }
}