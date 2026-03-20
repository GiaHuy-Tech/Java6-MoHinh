package com.example.demo.controllers;

import java.math.BigDecimal; // Import BigDecimal
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import com.example.demo.model.*;
import com.example.demo.repository.*;

@Controller
public class ProductsController {

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private CategoryRepository categoryRepo;

    @GetMapping("/products")
    public String productPage(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) BigDecimal minPrice, // Tham số mới
            @RequestParam(required = false) BigDecimal maxPrice, // Tham số mới
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model,
            HttpSession session) {

        // Xử lý sort
        Sort sorting = Sort.unsorted();
        if ("asc".equalsIgnoreCase(sort)) {
            sorting = Sort.by("price").ascending();
        } else if ("desc".equalsIgnoreCase(sort)) {
            sorting = Sort.by("price").descending();
        }

        Pageable pageable = PageRequest.of(page < 0 ? 0 : page, size, sorting);
        Page<Products> productPage;

        // Thiết lập giá mặc định nếu để trống để tránh lỗi query
        BigDecimal min = (minPrice == null) ? BigDecimal.ZERO : minPrice;
        BigDecimal max = (maxPrice == null) ? new BigDecimal("999999999") : maxPrice;

        // --- LOGIC LỌC TỔNG HỢP ---
        boolean hasKeyword = (keyword != null && !keyword.isBlank());
        boolean hasCategory = (categoryId != null);
        boolean hasPriceFilter = (minPrice != null || maxPrice != null);

        if (hasPriceFilter) {
            if (hasKeyword && hasCategory) {
                productPage = productRepo.findByCategoryIdAndNameContainingIgnoreCaseAndPriceBetween(categoryId, keyword, min, max, pageable);
            } else if (hasKeyword) {
                productPage = productRepo.findByNameContainingIgnoreCaseAndPriceBetween(keyword, min, max, pageable);
            } else if (hasCategory) {
                productPage = productRepo.findByCategoryIdAndPriceBetween(categoryId, min, max, pageable);
            } else {
                productPage = productRepo.findByPriceBetween(min, max, pageable);
            }
        } else {
            // Logic cũ khi không có lọc giá
            if (hasKeyword && hasCategory) {
                productPage = productRepo.findByCategoryIdAndNameContainingIgnoreCase(categoryId, keyword, pageable);
            } else if (hasKeyword) {
                productPage = productRepo.findByNameContainingIgnoreCase(keyword, pageable);
            } else if (hasCategory) {
                productPage = productRepo.findByCategoryId(categoryId, pageable);
            } else {
                productPage = productRepo.findByAvailableTrue(pageable);
            }
        }

        // Truyền dữ liệu ra view
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("categories", categoryRepo.findAll());

        // Giữ lại các giá trị filter để hiển thị trên Form
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);

        // Hiển thị User
        Account user = (Account) session.getAttribute("account");
        model.addAttribute("user", user);

        return "client/products";
    }
}