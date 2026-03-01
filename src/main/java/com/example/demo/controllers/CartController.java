package com.example.demo.controllers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Account;
import com.example.demo.model.CartDetail;
import com.example.demo.model.Products;
import com.example.demo.repository.CartDetailRepository;
import com.example.demo.repository.ProductRepository; // Đúng tên file của bạn

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartDetailRepository cartDetailRepo;
    
    @Autowired
    private ProductRepository productRepo; 

    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        // Lấy account từ session (check cả 2 tên phổ biến)
        Account account = (Account) session.getAttribute("account");
        if (account == null) account = (Account) session.getAttribute("user");

        if (account == null) return "redirect:/login";

        // Lấy data trực tiếp từ CartDetailRepo
        List<CartDetail> cartList = cartDetailRepo.findByAccount_Id(account.getId());
        
        BigDecimal total = BigDecimal.ZERO;
        for (CartDetail item : cartList) {
            if (item.getProduct() != null && item.getProduct().getPrice() != null) {
                BigDecimal price = item.getProduct().getPrice();
                BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
                total = total.add(price.multiply(qty));
            }
        }

        model.addAttribute("cartDetails", cartList);
        model.addAttribute("total", total);
        return "client/cart"; 
    }

    @GetMapping("/add/{productId}")
    @ResponseBody
    public String addToCart(@PathVariable Integer productId, HttpSession session) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) account = (Account) session.getAttribute("user");
        
        if (account == null) return "unauthorized";

        // Tìm món hàng cũ trong giỏ của user này
        List<CartDetail> existingItems = cartDetailRepo.findByAccount_Id(account.getId());
        CartDetail cartItem = existingItems.stream()
                .filter(item -> item.getProduct() != null && item.getProduct().getId().equals(productId))
                .findFirst().orElse(null);

        if (cartItem != null) {
            cartItem.setQuantity(cartItem.getQuantity() + 1);
            cartDetailRepo.save(cartItem);
        } else {
            Products product = productRepo.findById(productId).orElse(null);
            if (product != null) {
                CartDetail newItem = new CartDetail();
                newItem.setAccount(account);
                newItem.setProduct(product);
                newItem.setQuantity(1);
                newItem.setCreateDate(new Date());
                cartDetailRepo.save(newItem);
            }
        }
        return "success";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        cartDetailRepo.deleteById(id);
        return "redirect:/cart";
    }
}