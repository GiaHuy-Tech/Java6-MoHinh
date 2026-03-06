package com.example.demo.controllers;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    // ================= 1. TRANG GIỎ HÀNG CHÍNH (TRẢ VỀ GIAO DIỆN HTML) =================
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

        model.addAttribute("cartDetails", cartList);
        model.addAttribute("total", total);
        return "client/cart";
    }

    // ================= 2. API: THÊM VÀO GIỎ HÀNG (DÙNG CHO FETCH JS) =================
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
            return "error: " + e.getMessage();
        }
    }

    // ================= 3. API: LẤY DATA CHO MINI CART (TRẢ VỀ JSON) =================
    @GetMapping("/api/mini-cart")
    @ResponseBody
    public ResponseEntity<List<CartDetail>> getMiniCartData(HttpSession session) {
        Account account = getAccount(session);
        if (account == null) return ResponseEntity.status(401).build();

        // Lấy danh sách cart từ DB và trả thẳng về dạng JSON cho JS đọc
        List<CartDetail> cartList = cartRepo.findCartWithProduct(account.getId());
        return ResponseEntity.ok(cartList);
    }

    // ================= 4. API: LIVE SEARCH THẬT TỪ CSDL (TRẢ VỀ JSON) =================
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<List<Products>> liveSearch(@RequestParam("keyword") String keyword) {
        if (keyword == null || keyword.trim().length() < 2) {
            return ResponseEntity.badRequest().build(); // Không search nếu gõ quá ngắn
        }
        
        // Trả về top 5 sản phẩm khớp tên (từ Repository vừa thêm)
        List<Products> results = productRepo.findTop5ByNameContainingIgnoreCase(keyword.trim());
        return ResponseEntity.ok(results);
    }

    // ================= CÁC HÀM XỬ LÝ NÚT TĂNG/GIẢM/XÓA TRONG TRANG CART =================
    @GetMapping("/plus/{id}")
    public String increase(@PathVariable Integer id, HttpSession session) {
        Account account = getAccount(session);
        if (account == null) return "redirect:/login";
        CartDetail item = cartRepo.findById(id).orElse(null);
        if (item != null && item.getAccount().getId().equals(account.getId())) {
            item.setQuantity(item.getQuantity() + 1);
            cartRepo.save(item);
        }
        return "redirect:/cart";
    }

    @GetMapping("/minus/{id}")
    public String decrease(@PathVariable Integer id, HttpSession session) {
        Account account = getAccount(session);
        if (account == null) return "redirect:/login";
        CartDetail item = cartRepo.findById(id).orElse(null);
        if (item != null && item.getAccount().getId().equals(account.getId())) {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                cartRepo.save(item);
            } else {
                cartRepo.delete(item);
            }
        }
        return "redirect:/cart";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, HttpSession session) {
        Account account = getAccount(session);
        if (account == null) return "redirect:/login";
        CartDetail item = cartRepo.findById(id).orElse(null);
        if (item != null && item.getAccount().getId().equals(account.getId())) {
            cartRepo.delete(item);
        }
        return "redirect:/cart";
    }

    // Hàm tiện ích lấy User hiện tại
    private Account getAccount(HttpSession session) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) account = (Account) session.getAttribute("user");
        return account;
    }
}