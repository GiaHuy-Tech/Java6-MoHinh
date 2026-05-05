package com.example.demo.controllers;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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

    // Hàm phụ để lấy Account từ session linh hoạt
    private Account getSessionUser(HttpSession session) {
        Account user = (Account) session.getAttribute("account");
        if (user == null) {
			user = (Account) session.getAttribute("user");
		}
        return user;
    }

    // 1. Trang danh sách yêu thích
    @GetMapping
    public String wishlist(Model model, HttpSession session) {
        Account currentUser = getSessionUser(session);
        if (currentUser == null) {
			return "redirect:/login";
		}

        // Gọi hàm findByAccountId vừa thêm vào Repository
        List<Like> likes = likeRepo.findByAccountId(currentUser.getId());

        model.addAttribute("likes", likes);
        return "client/wishlist";
    }

    // 2. Logic Toggle (Thả tim/Bỏ tim)
    @GetMapping("/toggle/{productId}")
    @ResponseBody
    public ResponseEntity<String> toggleWishlist(@PathVariable Integer productId, HttpSession session) {
        Account user = getSessionUser(session);
        if (user == null) {
			return ResponseEntity.ok("unauthorized");
		}

        Like existingLike = likeRepo.findByAccountIdAndProductId(user.getId(), productId);

        if (existingLike != null) {
            likeRepo.delete(existingLike);
            return ResponseEntity.ok("removed");
        } else {
            Products product = productRepo.findById(productId).orElse(null);
            if (product == null) {
				return ResponseEntity.badRequest().body("error");
			}

            Like like = new Like();
            like.setProduct(product);
            like.setAccount(user);
            like.setLikedAt(new Date());
            likeRepo.save(like);
            return ResponseEntity.ok("added");
        }
    }

    // 3. Xóa trực tiếp từ trang wishlist.html
    @GetMapping("/remove/{id}")
    public String removeFromWishlist(@PathVariable Integer id, RedirectAttributes ra, HttpSession session) {
        Account user = getSessionUser(session);
        Like like = likeRepo.findById(id).orElse(null);

        if (like != null && user != null && like.getAccount().getId().equals(user.getId())) {
            likeRepo.delete(like);
            ra.addFlashAttribute("message", "Đã xóa khỏi danh sách yêu thích!");
        }
        return "redirect:/wishlist";
    }

    // 4. API lấy ID để tô màu tim ở các trang khác
    @GetMapping("/my-likes")
    @ResponseBody
    public ResponseEntity<List<Integer>> getMyLikedProductIds(HttpSession session) {
        Account user = getSessionUser(session);
        if (user == null) {
			return ResponseEntity.ok(Collections.emptyList());
		}

        List<Integer> likedIds = likeRepo.findByAccountId(user.getId()).stream()
                .filter(l -> l.getProduct() != null)
                .map(l -> l.getProduct().getId())
                .toList();
        return ResponseEntity.ok(likedIds);
    }
}