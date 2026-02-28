package com.example.demo.controllers;

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

        Account user = (Account) session.getAttribute("account");
        model.addAttribute("user", user);

        model.addAttribute("categories", categoryRepo.findAll());

        List<Products> latestProducts = productsRepo
                .findAll(PageRequest.of(0, 8, Sort.by("id").descending()))
                .getContent();

        model.addAttribute("latestProducts", latestProducts);

        return "client/index";
    }

    @GetMapping("/api/products/search")
    @ResponseBody
    public List<Products> searchProducts(@RequestParam("keyword") String keyword) {

        if (keyword == null || keyword.trim().length() < 1) {
            return List.of();
        }

        return productsRepo.findByNameContainingIgnoreCase(keyword);
    }
}