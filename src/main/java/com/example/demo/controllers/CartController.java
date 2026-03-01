package com.example.demo.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Account;
import com.example.demo.model.CartDetail;
import com.example.demo.repository.CartDetailRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartDetailRepository cartDetailRepo;

    // ================= VIEW CART =================
    @GetMapping
    public String viewCart(HttpSession session, Model model) {

        Account account = (Account) session.getAttribute("account");

        if (account == null) {
            return "redirect:/login";
        }

        List<CartDetail> cartList =
                cartDetailRepo.findByAccount_Id(account.getId());

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

    // ================= DELETE ITEM =================
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {

        cartDetailRepo.deleteById(id);
        return "redirect:/cart";
    }
}