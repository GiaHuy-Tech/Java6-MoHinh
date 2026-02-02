package com.example.demo.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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

        // ================= 1. XỬ LÝ USER LOGIN =================
        Account user = (Account) session.getAttribute("loggedInUser");
        if (user != null) {
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("user", user);
        } else {
            model.addAttribute("isLoggedIn", false);
        }

        // ================= 2. LẤY DANH MỤC =================
        model.addAttribute("categories", categoryRepo.findAll());

        // ================= 3. SẢN PHẨM MỚI NHẤT (TOP 5) =================
        // Dùng hàm có sẵn trong Repo của bạn: Lấy 5 sản phẩm mới tạo gần đây nhất
        // Thích hợp để hiển thị ở Banner hoặc Slider đầu trang
        List<Products> top5Newest = productsRepo.findTop5ByOrderByCreatedDateDesc();
        model.addAttribute("featuredProducts", top5Newest);

        // ================= 4. DANH SÁCH SẢN PHẨM (Lấy 8 cái) =================
        // Vì Repo của bạn chỉ có findAllByOrderByIdDesc() (lấy tất cả giảm dần theo ID)
        // Nên ta lấy hết về, sau đó dùng Java cắt lấy 8 cái đầu tiên

        List<Products> allProducts = productsRepo.findAllByOrderByIdDesc();
        List<Products> latest8Products = new ArrayList<>();

        if (allProducts.size() > 8) {
            latest8Products = allProducts.subList(0, 8);
        } else {
            latest8Products = allProducts;
        }

        model.addAttribute("latestProducts", latest8Products);

        return "client/index";
    }
}