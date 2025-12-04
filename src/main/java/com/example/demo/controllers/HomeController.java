package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.model.Products;
import com.example.demo.model.Account;

import jakarta.servlet.http.HttpSession;

import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private CategoryRepository categoryRepo;

    @Autowired
    private ProductRepository productsRepo;

    @GetMapping("/")
    public String home(Model model, HttpSession session) {

        // --- LẤY USER TỪ SESSION ---
        Account user = (Account) session.getAttribute("loggedInUser");
        if (user != null) {
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("user", user);
        } else {
            model.addAttribute("isLoggedIn", false);
        }

        // --- LẤY DANH MỤC ---
        model.addAttribute("categories", categoryRepo.findAll());

        // --- LẤY SẢN PHẨM NỔI BẬT ---
        List<Products> featured = productsRepo.findByAvailableTrue();
        model.addAttribute("featuredProducts", featured);

        // --- LẤY 8 SẢN PHẨM MỚI NHẤT ---
        List<Products> latestProducts = productsRepo.findAllByOrderByIdDesc();
        if (latestProducts.size() > 8) {
            latestProducts = latestProducts.subList(0, 8);
        }
        model.addAttribute("latestProducts", latestProducts);

        return "client/index";
    }
}
