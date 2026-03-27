package com.example.demo.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.*;
import com.example.demo.repository.*;

import jakarta.servlet.http.HttpSession;

@Controller
public class DetailController {

    @Autowired private ProductRepository productRepo;
    @Autowired private ProductImageRepository productImageRepo;
    @Autowired private CommentRepository commentRepo;
    @Autowired private CartDetailRepository cartDetailRepo;
    @Autowired private HttpSession session;

    // ================= PRODUCT DETAIL =================
    @GetMapping("/product-detail/{id}")
    public String productDetail(@PathVariable Integer id, Model model) {

        // 🔥 FIX: dùng JOIN FETCH
        Products product = productRepo.findByIdWithImages(id);
        if (product == null) return "redirect:/products";

        model.addAttribute("product", product);

        // Load ảnh phụ
        List<ProductImage> images = productImageRepo.findByProduct_Id(id);
        model.addAttribute("extraImages", images);

        // Load comment
        model.addAttribute("comments",
                commentRepo.findByProduct_IdOrderByCreatedAtDesc(id));

        return "client/product-detail";
    }

    // ================= ADD TO CART =================
    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Integer productId,
                           @RequestParam Integer quantity,
                           RedirectAttributes redirectAttributes) {

        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        Products product = productRepo.findById(productId).orElse(null);
        if (product == null) return "redirect:/products";

        cartDetailRepo.findByAccountAndProduct(account, product).ifPresentOrElse(
            detail -> {
                detail.setQuantity(detail.getQuantity() + quantity);
                cartDetailRepo.save(detail);
            },
            () -> {
                CartDetail newDetail = new CartDetail();
                newDetail.setAccount(account);
                newDetail.setProduct(product);
                newDetail.setQuantity(quantity);
                cartDetailRepo.save(newDetail);
            }
        );

        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm vào giỏ hàng!");
        return "redirect:/cart";
    }
}