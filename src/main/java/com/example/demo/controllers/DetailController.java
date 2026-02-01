package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.*;
import com.example.demo.repository.*;

import jakarta.servlet.http.HttpSession;

@Controller
public class DetailController {

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private CommentRepository commentRepo;

    @Autowired
    private OrdersDetailRepository orderDetailRepo;

    @Autowired
    private HttpSession session;

    // ================== GET PRODUCT DETAIL ==================
    @GetMapping("/product-detail/{id}")
    public String productDetail(
            @PathVariable Integer id,
            Model model) {

        Account account = (Account) session.getAttribute("account");
        Products product = productRepo.findById(id).orElse(null);
        if (product == null) return "redirect:/products";

        boolean canComment = false;

        if (account != null) {
            boolean purchased = orderDetailRepo.hasPurchased(account.getId(), id);
            boolean completed = orderDetailRepo.hasCompletedOrder(account.getId(), id);
            boolean commented = commentRepo.existsByAccount_IdAndProduct_Id(account.getId(), id);

            // ✅ Mua OR hoàn tất && chưa comment
            canComment = (purchased || completed) && !commented;
        }

        model.addAttribute("product", product);
        model.addAttribute("finalPrice", product.getPrice());
        model.addAttribute("canComment", canComment);
        model.addAttribute("comments",
                commentRepo.findByProduct_IdOrderByCreatedAtDesc(id));

        return "client/product-detail";
    }

    // ================== POST COMMENT ==================
    @PostMapping("/product-detail/comment/{productId}")
    public String postComment(
            @PathVariable Integer productId,
            @RequestParam String content,
            @RequestParam Integer rating,
            @RequestParam MultipartFile imageFile,
            RedirectAttributes redirect) {

        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        // ❌ Đã comment
        if (commentRepo.existsByAccount_IdAndProduct_Id(account.getId(), productId)) {
            redirect.addFlashAttribute("error", "Bạn đã đánh giá sản phẩm này rồi.");
            return "redirect:/product-detail/" + productId;
        }

        boolean purchased = orderDetailRepo.hasPurchased(account.getId(), productId);
        boolean completed = orderDetailRepo.hasCompletedOrder(account.getId(), productId);

        // ❌ Chưa mua
        if (!purchased && !completed) {
            redirect.addFlashAttribute("error", "Bạn cần mua sản phẩm để đánh giá.");
            return "redirect:/product-detail/" + productId;
        }

        Products product = productRepo.findById(productId).orElse(null);
        if (product == null) return "redirect:/products";

        Comment cmt = new Comment();
        cmt.setAccount(account);
        cmt.setProduct(product);
        cmt.setContent(content);
        cmt.setRating(rating);
        cmt.setCreatedAt(LocalDateTime.now());

        // Upload ảnh
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                Path path = Paths.get("uploads/comments");
                if (!Files.exists(path)) Files.createDirectories(path);
                Files.write(path.resolve(fileName), imageFile.getBytes());
                cmt.setImage(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        commentRepo.save(cmt);
        redirect.addFlashAttribute("success", "Đánh giá đã được gửi!");
        return "redirect:/product-detail/" + productId;
    }
}
