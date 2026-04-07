package com.example.demo.controllers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    @Autowired
    private CartDetailRepository cartRepo; // 🔥 THÊM

    @GetMapping("/products")
    public String productPage(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model,
            HttpSession session) {

        // ===== SORT =====
        Sort sorting = Sort.unsorted();
        if ("asc".equalsIgnoreCase(sort)) {
            sorting = Sort.by("price").ascending();
        } else if ("desc".equalsIgnoreCase(sort)) {
            sorting = Sort.by("price").descending();
        }

        Pageable pageable = PageRequest.of(page < 0 ? 0 : page, size, sorting);
        Page<Products> productPage;

        // ===== PRICE DEFAULT =====
        BigDecimal min = (minPrice == null) ? BigDecimal.ZERO : minPrice;
        BigDecimal max = (maxPrice == null) ? new BigDecimal("999999999") : maxPrice;

        boolean hasKeyword = (keyword != null && !keyword.isBlank());
        boolean hasCategory = (categoryId != null);
        boolean hasPriceFilter = (minPrice != null || maxPrice != null);

        // ===== FILTER LOGIC =====
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

        // ===== DATA =====
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("categories", categoryRepo.findAll());

        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);

        // ===== 🔥 FIX LOGIN (QUAN TRỌNG NHẤT) =====
        Account user = (Account) session.getAttribute("account");
        if (user == null) {
            user = (Account) session.getAttribute("user");
        }
        model.addAttribute("user", user);

        // ===== 🔥 FIX MINI CART =====
        List<CartDetail> cart = new ArrayList<>();

        if (user != null) {
            cart = cartRepo.findCartWithProduct(user.getId());
        }

        model.addAttribute("cart", cart);
        model.addAttribute("cartSize", cart.size());

        return "client/products";
    }
}