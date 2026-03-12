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
import com.example.demo.model.ProductImage;
import com.example.demo.model.Products;
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

        // USER SESSION
        Account user = (Account) session.getAttribute("account");
        model.addAttribute("user", user);

        // SIDEBAR ACTIVE
        model.addAttribute("activePage", "home");

        // BANNER
        List<BannerDTO> banners = new ArrayList<>();

        banners.add(new BannerDTO(
                "Gundam Universe",
                "Bộ sưu tập Gunpla mới nhất.",
                "https://images.unsplash.com/photo-1612036782180-6f0b6cd846fe?q=80&w=1600",
                "/category/gundam"));

        banners.add(new BannerDTO(
                "Limited Figures",
                "Mô hình giới hạn.",
                "https://images.unsplash.com/photo-1594787318286-3d835c1d207f?q=80&w=1600",
                "/category/figure"));

        model.addAttribute("banners", banners);

        // CATEGORY
        model.addAttribute("featuredCategories", categoryRepo.findAll());

        // LATEST PRODUCTS
        List<Products> latestProducts = productsRepo
                .findAll(PageRequest.of(0, 8, Sort.by("id").descending()))
                .getContent();

        // FIX ĐƯỜNG DẪN ẢNH
        for (Products p : latestProducts) {

            if (p.getImages() != null) {

                for (ProductImage img : p.getImages()) {

                    if (img.getImage() != null && !img.getImage().startsWith("/images/")) {

                        img.setImage("/images/products/" + img.getImage());
                    }
                }
            }
        }

        model.addAttribute("latestProducts", latestProducts);

        return "client/index";
    }

    // API SEARCH
    @GetMapping("/api/products/search")
    @ResponseBody
    public List<Products> searchProducts(
            @RequestParam(value = "keyword", required = false) String keyword) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return productsRepo.findByNameContainingIgnoreCase(keyword);
    }

    // BANNER DTO
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

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public String getLinkUrl() {
            return linkUrl;
        }
    }
}