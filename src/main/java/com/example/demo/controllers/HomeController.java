package com.example.demo.controllers;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
        // Xử lý login
        Account user = (Account) session.getAttribute("loggedInUser");
        model.addAttribute("isLoggedIn", user != null);
        
        // Danh mục
        model.addAttribute("categories", categoryRepo.findAll());

        // Sản phẩm mới nhất (Top 8)
        List<Products> allProducts = productsRepo.findAllByOrderByIdDesc();
        List<Products> latest8Products = allProducts.size() > 8 ? allProducts.subList(0, 8) : allProducts;
        model.addAttribute("latestProducts", latest8Products);

        return "client/index";
    }

    @GetMapping("/api/products/search")
    @ResponseBody
    public List<Products> searchProducts(@RequestParam("keyword") String keyword) {
        // Nếu keyword rỗng hoặc quá ngắn thì không tìm để tránh nặng máy
        if (keyword == null || keyword.trim().length() < 1) {
            return new ArrayList<>();
        }

        // Lấy TẤT CẢ các sản phẩm thỏa mãn điều kiện
        List<Products> list = productsRepo.findByNameContainingIgnoreCase(keyword);
        
        // Bạn có thể log ra console để kiểm tra xem Server đã lấy đúng chưa
        System.out.println("Tìm thấy: " + list.size() + " sản phẩm cho từ khóa: " + keyword);
        
        return list; 
    }
}