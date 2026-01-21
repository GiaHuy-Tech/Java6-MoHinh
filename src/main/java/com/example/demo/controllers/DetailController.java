package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private VoucherRepository voucherRepo;

    @Autowired
    private CommentRepository commentRepo;
    
    @Autowired
    private OrdersDetailRepository orderDetailRepo; 
    
    @Autowired
    private HttpSession session;

    @GetMapping("/product-detail/{id}")
    public String productDetail(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer voucherId,
            Model model) {

        Account currentAccount = (Account) session.getAttribute("account");
        Products product = productRepo.findById(id).orElse(null);
        if (product == null) return "redirect:/products";

        // ... (Logic xử lý Voucher giữ nguyên như cũ) ...
        double finalPrice = product.getPrice();
        // (Copy lại phần logic Voucher từ câu trả lời trước của mình nếu cần)

        // --- KIỂM TRA QUYỀN COMMENT ---
        boolean canComment = false;
        if (currentAccount != null) {
            canComment = orderDetailRepo.existsByAccountAndProduct(currentAccount.getId(), id);
        }

        model.addAttribute("product", product);
        model.addAttribute("finalPrice", finalPrice);
        model.addAttribute("canComment", canComment);
        
        // --- 🔥 SỬA DÒNG NÀY ĐỂ KHỚP VỚI REPOSITORY MỚI 🔥 ---
        // Đổi từ ...CreatedDate... thành ...CreatedAt...
        model.addAttribute("comments", commentRepo.findByProduct_IdOrderByCreatedAtDesc(id));

        return "client/product-detail";
    }

    @PostMapping("/product-detail/comment/{productId}")
    public String postComment(
            @PathVariable Integer productId,
            @RequestParam("content") String content,
            @RequestParam("rating") Integer rating,
            @RequestParam("imageFile") MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {
        
        // ... (Giữ nguyên logic post comment như cũ) ...
        Account currentAccount = (Account) session.getAttribute("account");
        if (currentAccount == null) return "redirect:/login";

        Products product = productRepo.findById(productId).orElse(null);
        if (product != null) {
            Comment comment = new Comment();
            comment.setAccount(currentAccount);
            comment.setProduct(product);
            comment.setContent(content);
            comment.setRating(rating);
            
            // Model của bạn dùng createdAt
            comment.setCreatedAt(LocalDateTime.now()); 
            
            // Xử lý ảnh...
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                    Path path = Paths.get("uploads/comments/");
                    if (!Files.exists(path)) Files.createDirectories(path);
                    Files.write(path.resolve(fileName), imageFile.getBytes());
                    comment.setImage(fileName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            commentRepo.save(comment);
        }
        return "redirect:/product-detail/" + productId;
    }
}