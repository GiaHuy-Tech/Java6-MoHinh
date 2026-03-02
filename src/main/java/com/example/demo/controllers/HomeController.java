package com.example.demo.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.model.Account;
import com.example.demo.model.Products;
// import com.example.demo.model.Banner; // Nếu sau này có Entity Banner thật thì mở ra
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    @Autowired
    private CategoryRepository categoryRepo;

    @Autowired
    private ProductRepository productsRepo;

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        
        // 1. Xử lý User session
        Account user = (Account) session.getAttribute("account");
        model.addAttribute("user", user);

        // 2. Active Page (Để Sidebar biết đang ở trang nào mà sáng đèn)
        model.addAttribute("activePage", "home");

        // 3. Banner Slideshow (Dữ liệu giả lập để test giao diện)
        // Sau này bạn tạo bảng Banner trong DB thì thay đoạn này bằng bannerRepo.findAll()
        List<BannerDTO> banners = new ArrayList<>();
        banners.add(new BannerDTO("Gundam Universe", "Bộ sưu tập Gunpla mới nhất.", "https://images.unsplash.com/photo-1612036782180-6f0b6cd846fe?q=80&w=1600", "/category/gundam"));
        banners.add(new BannerDTO("Limited Figures", "Mô hình giới hạn.", "https://images.unsplash.com/photo-1560167016-01dfb00dc948?q=80&w=1600", "/category/figure"));
        model.addAttribute("banners", banners);

        // 4. Danh mục nổi bật (Đổi tên biến từ 'categories' -> 'featuredCategories' cho khớp HTML)
        // Lấy 3-6 danh mục đầu tiên
        model.addAttribute("featuredCategories", categoryRepo.findAll()); 

        // 5. Sản phẩm mới nhất (Lấy 8 sản phẩm mới nhất theo ID giảm dần)
        List<Products> latestProducts = productsRepo
                .findAll(PageRequest.of(0, 8, Sort.by("id").descending()))
                .getContent();
        model.addAttribute("latestProducts", latestProducts);

        return "client/index";
    }

    // API Search (Giữ nguyên logic cũ nhưng check kỹ null)
    @GetMapping("/api/products/search")
    @ResponseBody
    public List<Products> searchProducts(@RequestParam(value = "keyword", required = false) String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return productsRepo.findByNameContainingIgnoreCase(keyword);
    }

    // --- DTO Class nội bộ để chạy Banner giả (Chỉ dùng cho mục đích hiển thị View) ---
    public static class BannerDTO {
        public String title;
        public String description;
        public String imageUrl;
        public String linkUrl;

        public BannerDTO(String title, String description, String imageUrl, String linkUrl) {
            this.title = title;
            this.description = description;
            this.imageUrl = imageUrl;
            this.linkUrl = linkUrl;
        }
    }
}