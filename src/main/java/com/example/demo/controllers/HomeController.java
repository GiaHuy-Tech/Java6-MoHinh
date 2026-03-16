package com.example.demo.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.model.Account;
import com.example.demo.model.CartDetail;
import com.example.demo.model.Products;
import com.example.demo.repository.CartDetailRepository;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    @Autowired
    private CategoryRepository categoryRepo;

    @Autowired
    private ProductRepository productsRepo;

    @Autowired
    private CartDetailRepository cartRepo;

    @GetMapping("/")
    public String home(Model model, HttpSession session) {

        // USER
        Account user = (Account) session.getAttribute("account");
        if (user == null) {
            user = (Account) session.getAttribute("user");
        }
        model.addAttribute("user", user);

        // ===== MINI CART FROM DATABASE =====
        List<CartDetail> cart = new ArrayList<>();

        if (user != null) {
            cart = cartRepo.findCartWithProduct(user.getId());
        }

        model.addAttribute("cart", cart);
        model.addAttribute("cartSize", cart.size());

        // CATEGORY
        model.addAttribute("featuredCategories", categoryRepo.findAll());

        // LATEST PRODUCTS
        List<Products> latestProducts = productsRepo
                .findAll(PageRequest.of(0, 8, Sort.by("id").descending()))
                .getContent();

        model.addAttribute("latestProducts", latestProducts);

        return "client/index";
    }
}