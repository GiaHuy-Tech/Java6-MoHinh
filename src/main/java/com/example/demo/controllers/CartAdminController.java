package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.repository.CartDetailRepository;

@Controller
@RequestMapping("/admin/cart")
public class CartAdminController {

    @Autowired
    private CartDetailRepository cartDetailRepo;

    @GetMapping
    public String stats(Model model) {

        model.addAttribute("cartStats", cartDetailRepo.getTopProductsInCarts());

        long totalItems = cartDetailRepo.findAll().stream()
                .mapToInt(c -> c.getQuantity())
                .sum();

        model.addAttribute("totalItemsInCarts", totalItems);

        return "admin/cart-list";
    }
}