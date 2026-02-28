package com.example.demo.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.CartDetail;
import com.example.demo.repository.CartDetailRepository;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartDetailRepository cartDetailRepo;

    // ===== XEM GIỎ HÀNG =====
    @GetMapping("/{accountId}")
    public String viewCart(@PathVariable Integer accountId, Model model) {

        List<CartDetail> cartList =
                cartDetailRepo.findByAccount_Id(accountId);

        long total = 0;

        for (CartDetail item : cartList) {
            if (item.getProduct() != null) {
                total += item.getProduct().getPrice()
                        * item.getQuantity();
            }
        }

        model.addAttribute("cartList", cartList);
        model.addAttribute("total", total);

        return "cart/view";
    }

    // ===== XOÁ SẢN PHẨM KHỎI GIỎ =====
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {

        cartDetailRepo.deleteById(id);
        return "redirect:/cart/1";
    }
}