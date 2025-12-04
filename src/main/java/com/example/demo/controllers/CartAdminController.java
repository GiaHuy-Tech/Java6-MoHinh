package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Cart;
import com.example.demo.repository.CartRepository;

@Controller
@RequestMapping("/admin/cart")
public class CartAdminController {

    @Autowired
    private CartRepository cartRepo;

    // 📄 Trang danh sách giỏ hàng
    @GetMapping
    public String list(Model model) {
        model.addAttribute("carts", cartRepo.findAll());
        return "admin/cart-list";
    }

    // 📦 Trang chi tiết giỏ hàng
    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Integer id, Model model) {
        Cart cart = cartRepo.findById(id).orElse(null);
        if (cart == null) {
            model.addAttribute("errorMessage", "Không tìm thấy giỏ hàng với ID " + id);
            model.addAttribute("carts", cartRepo.findAll()); // hiển thị lại danh sách
            return "admin/cart-list"; 
        }
        model.addAttribute("cart", cart);
        return "admin/cart-detail";
    }
}
