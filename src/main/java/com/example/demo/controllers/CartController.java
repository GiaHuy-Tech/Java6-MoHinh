package com.example.demo.controllers;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Account;
import com.example.demo.model.CartDetail;
import com.example.demo.model.Products;
import com.example.demo.repository.CartDetailRepository; // Repo bạn vừa đưa
import com.example.demo.repository.ProductRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartDetailRepository cartRepo; // Tên biến khớp với ý bạn

    @Autowired
    private ProductRepository productRepo;

    // ================= VIEW CART =================
    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        Account account = getAccount(session);
        if (account == null) return "redirect:/login";

        // Gọi hàm từ CartDetailRepository
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

    // ================= ADD TO CART (FETCH) =================
    @PostMapping("/add/{productId}")
    @ResponseBody
    public String addToCart(@PathVariable Integer productId, HttpSession session) {
        Account account = getAccount(session);
        if (account == null) return "unauthorized";

        Products product = productRepo.findById(productId).orElse(null);
        if (product == null) return "error";

        // Sử dụng cartRepo (kiểu CartDetailRepository)
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
            
            // QUAN TRỌNG: Nếu DB vẫn báo lỗi "cart_id NULL", 
            // bạn hãy kiểm tra xem trong Model CartDetail có trường cartId không.
            // Nếu có, hãy gán tạm: cartItem.setCartId(1); hoặc xóa ràng buộc trong SQL.
        }

        try {
            cartRepo.save(cartItem);
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return "error: " + e.getMessage();
        }
    }

    // ================= CÁC HÀM CÒN LẠI =================
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

    private Account getAccount(HttpSession session) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) account = (Account) session.getAttribute("user");
        return account;
    }
}