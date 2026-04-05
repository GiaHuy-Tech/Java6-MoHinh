package com.example.demo.controllers;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.MiniCartDTO;
import com.example.demo.model.Account;
import com.example.demo.model.CartDetail;
import com.example.demo.model.Products;
import com.example.demo.repository.CartDetailRepository;
import com.example.demo.repository.ProductRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartDetailRepository cartRepo;

    @Autowired
    private ProductRepository productRepo;

    // ================= 1. TRANG GIỎ HÀNG =================
    @GetMapping
    public String viewCart(HttpSession session, Model model) {

        Account account = getAccount(session);
        if (account == null) return "redirect:/login";

        List<CartDetail> cartList = cartRepo.findCartWithProduct(account.getId());

        BigDecimal total = BigDecimal.ZERO;

        for (CartDetail item : cartList) {
            BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
            BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
            total = total.add(price.multiply(qty));
        }

        // ===== TRANG CART =====
        model.addAttribute("cartDetails", cartList);
        model.addAttribute("total", total);

        // ===== MINI CART =====
        model.addAttribute("cart", cartList);
        model.addAttribute("cartSize", cartList.size());

        return "client/cart";
    }

    // ================= 2. THÊM SẢN PHẨM =================
    @PostMapping("/add/{productId}")
    @ResponseBody
    public String addToCart(@PathVariable Integer productId, HttpSession session) {

        Account account = getAccount(session);
        if (account == null) return "unauthorized";

        Products product = productRepo.findById(productId).orElse(null);
        if (product == null) return "error";

        CartDetail cartItem = cartRepo.findByAccountAndProduct(account, product).orElse(null);

        if (cartItem != null) {
            cartItem.setQuantity(cartItem.getQuantity() + 1);
        } else {
            cartItem = new CartDetail();
            cartItem.setAccount(account);
            cartItem.setProduct(product);
            cartItem.setQuantity(1);
            cartItem.setCreateDate(new Date());
            cartItem.setPrice(product.getPrice());
        }

        try {
            cartRepo.save(cartItem);
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    // ================= 3. MINI CART API =================
    @GetMapping("/api/mini-cart")
    @ResponseBody
    public ResponseEntity<List<MiniCartDTO>> getMiniCartData(HttpSession session) {

        Account account = getAccount(session);
        if (account == null) return ResponseEntity.status(401).build();

        List<CartDetail> cartList = cartRepo.findCartWithProduct(account.getId());

        List<MiniCartDTO> result = cartList.stream()
            .map(MiniCartDTO::new)
            .toList();

        return ResponseEntity.ok(result);
    }

    // ================= 4. LIVE SEARCH =================
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<List<Products>> liveSearch(@RequestParam("keyword") String keyword) {

        if (keyword == null || keyword.trim().length() < 2) {
            return ResponseEntity.badRequest().build();
        }

        List<Products> results = productRepo.findTop5ByNameContainingIgnoreCase(keyword.trim());

        return ResponseEntity.ok(results);
    }

 // ================= API TĂNG =================
    @PostMapping("/plus/{id}")
    @ResponseBody
    public ResponseEntity<?> apiIncrease(@PathVariable Integer id, HttpSession session) {

        Account account = getAccount(session);
        if (account == null) return ResponseEntity.status(401).build();

        CartDetail item = cartRepo.findById(id).orElse(null);

        if (item != null && item.getAccount().getId().equals(account.getId())) {
            item.setQuantity(item.getQuantity() + 1);
            cartRepo.save(item);
        }

        return ResponseEntity.ok().build();
    }

    // ================= API GIẢM =================
    @PostMapping("/minus/{id}")
    @ResponseBody
    public ResponseEntity<?> apiDecrease(@PathVariable Integer id, HttpSession session) {

        Account account = getAccount(session);
        if (account == null) return ResponseEntity.status(401).build();

        CartDetail item = cartRepo.findById(id).orElse(null);

        if (item != null && item.getAccount().getId().equals(account.getId())) {

            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                cartRepo.save(item);
            } else {
                cartRepo.delete(item);
            }
        }

        return ResponseEntity.ok().build();
    }

    // ================= API XOÁ =================
    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> apiDelete(@PathVariable Integer id, HttpSession session) {

        Account account = getAccount(session);
        if (account == null) return ResponseEntity.status(401).build();

        CartDetail item = cartRepo.findById(id).orElse(null);

        if (item != null && item.getAccount().getId().equals(account.getId())) {
            cartRepo.delete(item);
        }

        return ResponseEntity.ok().build();
    }

    // ================= LẤY USER =================
    private Account getAccount(HttpSession session) {

        Account account = (Account) session.getAttribute("account");

        if (account == null) {
            account = (Account) session.getAttribute("user");
        }

        return account;
    }
}