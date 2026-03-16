package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession; // <-- THÊM IMPORT NÀY

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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model,
            HttpSession session) { // <-- THÊM HttpSession VÀO ĐÂY

        // Xử lý sort
        Sort sorting = Sort.unsorted();

        if ("asc".equalsIgnoreCase(sort)) {
            sorting = Sort.by("price").ascending();
        } else if ("desc".equalsIgnoreCase(sort)) {
            sorting = Sort.by("price").descending();
        }

        if (page < 0) {
            page = 0;
        }
        
        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<Products> productPage;

        // Filter logic (PHÂN TRANG TẠI DATABASE)
        if (keyword != null && !keyword.isBlank() && categoryId != null) {
            productPage = productRepo.findByCategoryIdAndNameContainingIgnoreCase(categoryId, keyword, pageable);
        } 
        else if (keyword != null && !keyword.isBlank()) {
            productPage = productRepo.findByNameContainingIgnoreCase(keyword, pageable);
        } 
        else if (categoryId != null) {
            productPage = productRepo.findByCategoryId(categoryId, pageable);
        } 
        else {
            productPage = productRepo.findByAvailableTrue(pageable);
        }

        // Truyền dữ liệu ra view
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("categories", categoryRepo.findAll());

        // Giữ lại filter khi chuyển trang
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedSort", sort);

        // ==========================================
        // ĐOẠN CODE SỬA LỖI HIỂN THỊ USER Ở ĐÂY
        // LƯU Ý: Chữ "user" trong session.getAttribute() phải đúng với key bạn đã set lúc Login
        Object currentUser = session.getAttribute("user"); 
        model.addAttribute("user", currentUser);
        // ==========================================

        return "client/products";
    }
}