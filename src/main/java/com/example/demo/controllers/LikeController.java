package com.example.demo.controllers;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Account;
import com.example.demo.model.Like;
import com.example.demo.model.Products;
import com.example.demo.repository.LikeRepository;
import com.example.demo.repository.ProductRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/wishlist")
public class LikeController {

    @Autowired
    private LikeRepository likeRepo;

    @Autowired
    private ProductRepository productRepo;

    // 1. Mở trang danh sách yêu thích
    @GetMapping
    public String wishlist(Model model, @ModelAttribute("message") String message, HttpSession session) {
        Account currentUser = (Account) session.getAttribute("account");
        if (currentUser == null) currentUser = (Account) session.getAttribute("user");
        if (currentUser == null) return "redirect:/login";

        final Integer currentUserId = currentUser.getId();
        List<Like> likes = likeRepo.findAllOrderByNewest().stream()
                .filter(like -> like.getProduct() != null && like.getAccount() != null && like.getAccount().getId().equals(currentUserId))
                .toList();
        model.addAttribute("likes", likes);
        if (message != null && !message.isEmpty()) model.addAttribute("message", message);
        return "client/wishlist";
    }

    // 2. Tự động kiểm tra: Thêm nếu chưa có, Xóa nếu đã có (Dành cho nút bấm)
    @GetMapping("/toggle/{productId}")
    @ResponseBody
    public ResponseEntity<String> toggleWishlistAjax(@PathVariable Integer productId, HttpSession session) {
        Account currentUser = (Account) session.getAttribute("account");
        if (currentUser == null) currentUser = (Account) session.getAttribute("user");
        if (currentUser == null) return ResponseEntity.ok("unauthorized");

        Products product = productRepo.findById(productId).orElse(null);
        if (product != null) {
            final Integer currentUserId = currentUser.getId();
            
            // Tìm xem tim này đã tồn tại chưa
            Like existingLike = likeRepo.findAll().stream()
                    .filter(like -> like.getProduct() != null && like.getProduct().getId().equals(product.getId())
                                 && like.getAccount() != null && like.getAccount().getId().equals(currentUserId))
                    .findFirst().orElse(null);

            if (existingLike != null) {
                // NẾU ĐÃ CÓ -> Xóa đi (Hủy tim)
                likeRepo.delete(existingLike);
                return ResponseEntity.ok("removed");
            } else {
                // NẾU CHƯA CÓ -> Thêm mới (Thả tim)
                Like like = new Like();
                like.setProduct(product);
                like.setAccount(currentUser);
                like.setLikedAt(new Date()); 
                likeRepo.save(like);
                return ResponseEntity.ok("added"); 
            }
        }
        return ResponseEntity.badRequest().body("error"); 
    }

    // 3. Lấy danh sách ID các sản phẩm đã thả tim (Để tô đỏ lúc F5 trang)
    @GetMapping("/my-likes")
    @ResponseBody
    public ResponseEntity<List<Integer>> getMyLikedProductIds(HttpSession session) {
        Account currentUser = (Account) session.getAttribute("account");
        if (currentUser == null) currentUser = (Account) session.getAttribute("user");
        if (currentUser == null) return ResponseEntity.ok(java.util.Collections.emptyList());

        final Integer currentUserId = currentUser.getId();
        List<Integer> likedIds = likeRepo.findAll().stream()
                .filter(like -> like.getAccount() != null && like.getAccount().getId().equals(currentUserId) && like.getProduct() != null)
                .map(like -> like.getProduct().getId())
                .toList();

        return ResponseEntity.ok(likedIds);
    }

    // Xóa từ trang wishlist.html
    @GetMapping("/remove/{id}")
    public String removeFromWishlist(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        Like like = likeRepo.findById(id).orElse(null);
        if (like != null) {
            likeRepo.delete(like);
            redirectAttrs.addFlashAttribute("message", "Đã xóa khỏi danh sách yêu thích!");
        }
        return "redirect:/wishlist";
    }
}