package com.example.demo.controllers;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Account;
import com.example.demo.model.CartDetail;
import com.example.demo.model.Products;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.CartDetailRepository;
import com.example.demo.repository.ProductRepository;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartDetailRepository cartDetailRepo;

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private AccountRepository accountRepo;

    // ================= LẤY USER ĐANG LOGIN =================
    private Account getCurrentUser(Principal principal) {
        if (principal == null) return null;

        String email = principal.getName(); // Spring Security dùng email làm username
        return accountRepo.findByEmail(email).orElse(null);
    }

    // ================= XEM GIỎ HÀNG =================
    @GetMapping
    public String viewCart(Model model, Principal principal) {

        Account user = getCurrentUser(principal);

        if (user == null) {
            return "redirect:/login";
        }

        List<CartDetail> cartList = cartDetailRepo.findByAccount(user);

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

    // ================= THÊM SẢN PHẨM =================
    @GetMapping("/add/{productId}")
    @ResponseBody
    public String addToCart(@PathVariable Integer productId, Principal principal) {

        Account user = getCurrentUser(principal);
        if (user == null) return "unauthorized";

        Products product = productRepo.findById(productId).orElse(null);
        if (product == null) return "not_found";

        CartDetail cartItem = cartDetailRepo
                .findByAccountAndProduct(user, product)
                .orElse(null);

        if (cartItem != null) {
            cartItem.setQuantity(cartItem.getQuantity() + 1);
        } else {
            cartItem = new CartDetail();
            cartItem.setAccount(user);
            cartItem.setProduct(product);
            cartItem.setQuantity(1);
        }

        cartDetailRepo.save(cartItem);

        return "success";
    }

    // ================= XÓA =================
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, Principal principal) {

        Account user = getCurrentUser(principal);
        if (user == null) return "redirect:/login";

        CartDetail item = cartDetailRepo.findById(id).orElse(null);

        if (item != null && item.getAccount().getId().equals(user.getId())) {
            cartDetailRepo.delete(item);
        }

        return "redirect:/cart";
    }
}