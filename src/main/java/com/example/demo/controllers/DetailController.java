package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Account;
import com.example.demo.model.Comment;
import com.example.demo.model.Products;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.OrdersDetailRepository;
import com.example.demo.repository.ProductRepository;

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

    // ================== PRODUCT DETAIL ==================
    @GetMapping("/product-detail/{id}")
    public String productDetail(@PathVariable Integer id, Model model) {

        Account account = (Account) session.getAttribute("account");
        Products product = productRepo.findById(id).orElse(null);

        if (product == null) {
            return "redirect:/products";
        }

        boolean canComment = false;

        if (account != null) {
            boolean purchased = orderDetailRepo.hasPurchased(account.getId(), id);
            boolean completed = orderDetailRepo.hasCompletedOrder(account.getId(), id);
            boolean commented = commentRepo
                    .existsByAccount_IdAndProduct_Id(account.getId(), id);

            // Đã mua / hoàn tất và chưa comment
            canComment = (purchased || completed) && !commented;
        }

        model.addAttribute("product", product);
        model.addAttribute("canComment", canComment);
        model.addAttribute(
                "comments",
                commentRepo.findByProduct_IdOrderByCreatedAtDesc(id)
        );

        return "client/product-detail";
    }

    // ================== POST COMMENT ==================
    @PostMapping("/product-detail/comment/{productId}")
    public String postComment(
            @PathVariable Integer productId,
            @RequestParam String content,
            @RequestParam Integer rating,
            @RequestParam(required = false) MultipartFile imageFile,
            RedirectAttributes redirect) {

        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        // Đã comment rồi
        if (commentRepo.existsByAccount_IdAndProduct_Id(account.getId(), productId)) {
            redirect.addFlashAttribute("error", "Bạn đã đánh giá sản phẩm này rồi.");
            return "redirect:/product-detail/" + productId;
        }

        boolean purchased = orderDetailRepo.hasPurchased(account.getId(), productId);
        boolean completed = orderDetailRepo.hasCompletedOrder(account.getId(), productId);

        if (!purchased && !completed) {
            redirect.addFlashAttribute("error", "Bạn cần mua sản phẩm để đánh giá.");
            return "redirect:/product-detail/" + productId;
        }

        Products product = productRepo.findById(productId).orElse(null);
        if (product == null) {
            return "redirect:/products";
        }

        Comment comment = new Comment();
        comment.setAccount(account);
        comment.setProduct(product);
        comment.setContent(content);
        comment.setRating(rating);
        comment.setCreatedAt(LocalDateTime.now());

        // Upload ảnh comment
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String fileName = System.currentTimeMillis()
                        + "_" + imageFile.getOriginalFilename();

                Path uploadPath = Paths.get("uploads/comments");
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Files.write(uploadPath.resolve(fileName), imageFile.getBytes());
                comment.setImage(fileName);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        commentRepo.save(comment);
        redirect.addFlashAttribute("success", "Đánh giá đã được gửi!");

        return "redirect:/product-detail/" + productId;
    }
}
