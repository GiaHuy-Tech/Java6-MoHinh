package com.example.demo.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Like;
import com.example.demo.model.Products;
import com.example.demo.repository.LikeRepository;
import com.example.demo.repository.ProductRepository;

@Controller
@RequestMapping("/wishlist")
public class LikeController {

    @Autowired
    private LikeRepository likeRepo;

    @Autowired
    private ProductRepository productRepo;

    // ✅ Hiển thị danh sách wishlist
    @GetMapping
    public String wishlist(Model model, @ModelAttribute("message") String message) {
        List<Like> likes = likeRepo.findAllOrderByNewest().stream()
                .filter(like -> like.getProduct() != null)
                .toList();

        model.addAttribute("likes", likes);

        // Nếu có thông báo từ redirect thì hiển thị
        if (message != null && !message.isEmpty()) {
            model.addAttribute("message", message);
        }

        return "client/wishlist";
    }

    // ✅ Thêm sản phẩm vào wishlist
    @GetMapping("/add/{productId}")
    public String addToWishlist(@PathVariable Integer productId, RedirectAttributes redirectAttrs) {
        Products product = productRepo.findById(productId).orElse(null);
        if (product != null) {
            // Kiểm tra sản phẩm đã tồn tại trong wishlist chưa
            boolean exists = likeRepo.findAll().stream()
                    .anyMatch(like -> like.getProduct() != null 
                            && like.getProduct().getId().equals(product.getId()));

            if (exists) {
                redirectAttrs.addFlashAttribute("message", "Sản phẩm này bạn đã like rồi ❤️");
                return "redirect:/wishlist";
            }

            // Nếu chưa có thì thêm mới
            Like like = new Like();
            like.setProduct(product);
            likeRepo.save(like);
            redirectAttrs.addFlashAttribute("message", "Đã thêm sản phẩm vào danh sách yêu thích!");
        }
        return "redirect:/wishlist";
    }

    // ✅ Xóa sản phẩm khỏi wishlist
    @GetMapping("/remove/{id}")
    public String removeFromWishlist(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        Like like = likeRepo.findById(id).orElse(null);
        if (like != null) {
            likeRepo.delete(like);
            redirectAttrs.addFlashAttribute("message", "Đã xóa khỏi danh sách yêu thích!");
        }
        return "redirect:/wishlist";
    }

    // ✅ Thêm vào giỏ hàng (tuỳ chọn)
    @GetMapping("/add-to-cart/{id}")
    public String addToCart(@PathVariable Integer id) {
        return "redirect:/cart";
    }
}
