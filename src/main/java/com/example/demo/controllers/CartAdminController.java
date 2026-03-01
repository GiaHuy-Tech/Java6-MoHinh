package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.CartDetail;
import com.example.demo.repository.CartDetailRepository;

@Controller
@RequestMapping("/admin/cart")
public class CartAdminController {

    @Autowired
    private CartDetailRepository cartDetailRepo;

    // ===== DANH SÁCH CART DETAIL =====
    @GetMapping
    public String list(Model model) {
        model.addAttribute("carts", cartDetailRepo.findAll());
        return "admin/cart-list";
    }

    // ===== CHI TIẾT =====
    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Integer id, Model model) {

        CartDetail cartDetail =
                cartDetailRepo.findById(id).orElse(null);

        if (cartDetail == null) {
            model.addAttribute("errorMessage",
                    "Không tìm thấy cart detail ID " + id);
            model.addAttribute("carts",
                    cartDetailRepo.findAll());
            return "admin/cart-list";
        }

        model.addAttribute("cart", cartDetail);
        return "admin/cart-detail";
    }
}