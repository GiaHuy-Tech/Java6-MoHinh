package com.example.demo.controllers;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
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
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String keyword,
            Model model) {

        List<Products> products = new ArrayList<>();

        // ✅ 1. TÌM THEO KEYWORD + DANH MỤC
        if (keyword != null && !keyword.isBlank() && categoryId != null) {
            products = productRepo.findByCategoryIdAndNameContainingIgnoreCase(categoryId, keyword);
        } 
        else if (keyword != null && !keyword.isBlank()) {
            products = productRepo.findByNameContainingIgnoreCase(keyword);
        } 
        else if (categoryId != null) {
            products = productRepo.findByCategoryId(categoryId);
        } 
        else {
            products = productRepo.findByAvailableTrue();
        }

        // ✅ 2. LỌC THEO GIÁ (nếu có)
        if (minPrice != null && maxPrice != null) {
            products = products.stream()
                    .filter(p -> p.getPrice() >= minPrice && p.getPrice() <= maxPrice)
                    .toList();
        }

        // ✅ 3. SẮP XẾP THEO GIÁ
        if ("asc".equalsIgnoreCase(sort)) {
            products.sort(Comparator.comparingDouble(Products::getPrice));
        } else if ("desc".equalsIgnoreCase(sort)) {
            products.sort(Comparator.comparingDouble(Products::getPrice).reversed());
        }

        // ✅ 4. TRUYỀN DỮ LIỆU RA VIEW
        List<Category> categories = categoryRepo.findAll();
        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("keyword", keyword);

        return "client/products";
    }
}
