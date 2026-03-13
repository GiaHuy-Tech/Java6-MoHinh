package com.example.demo.controllers;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;

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
    private ProductImageRepository productImageRepo;

    @Autowired
    private CommentRepository commentRepo;

    @Autowired
    private OrdersDetailRepository orderDetailRepo;

    @Autowired
    private CartDetailRepository cartDetailRepo;

    @Autowired
    private HttpSession session;

    // ================= PRODUCT DETAIL =================
    @GetMapping("/product-detail/{id}")
    public String productDetail(@PathVariable Integer id, Model model) {

        Account account = (Account) session.getAttribute("account");

        Products product = productRepo.findById(id).orElse(null);
        if (product == null) return "redirect:/products";

        model.addAttribute("product", product);

        model.addAttribute("extraImages",
                productImageRepo.findByProduct_Id(id));

        // ===== COMMENT PERMISSION =====
        boolean canComment = false;

        if (account != null) {

            boolean hasCompletedOrder =
                    orderDetailRepo.hasCompletedOrder(
                            account.getId(), id);

            boolean hasCommented =
                    commentRepo.existsByAccount_IdAndProduct_Id(
                            account.getId(), id);

            canComment = hasCompletedOrder && !hasCommented;
        }

        model.addAttribute("canComment", canComment);

        model.addAttribute("comments",
                commentRepo.findByProduct_IdOrderByCreatedAtDesc(id));

        return "client/product-detail";
    }


    // ================= ADD TO CART =================
    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Integer productId,
                            @RequestParam Integer quantity,
                            RedirectAttributes redirectAttributes) {

        Account account =
                (Account) session.getAttribute("account");

        if (account == null)
            return "redirect:/login";

        Products product =
                productRepo.findById(productId).orElse(null);

        if (product == null)
            return "redirect:/products";

        Optional<CartDetail> existing =
                cartDetailRepo.findByAccountAndProduct(account, product);

        if (existing.isPresent()) {

            CartDetail detail = existing.get();
            detail.setQuantity(detail.getQuantity() + quantity);
            cartDetailRepo.save(detail);

        } else {

            CartDetail newDetail = new CartDetail();
            newDetail.setAccount(account);
            newDetail.setProduct(product);
            newDetail.setQuantity(quantity);

            cartDetailRepo.save(newDetail);
        }

        redirectAttributes.addFlashAttribute(
                "successMessage", "Đã thêm vào giỏ hàng!");

        return "redirect:/cart";
    }


    // ================= POST COMMENT =================
    @PostMapping("/product-detail/comment/{productId}")
    public String postComment(@PathVariable Integer productId,
                              @RequestParam String content,
                              @RequestParam Integer rating,
                              @RequestParam(required = false)
                              MultipartFile imageFile) {

        Account account =
                (Account) session.getAttribute("account");

        if (account == null)
            return "redirect:/login";

        Products product =
                productRepo.findById(productId).orElse(null);

        boolean hasCompletedOrder =
                orderDetailRepo.hasCompletedOrder(
                        account.getId(), productId);

        boolean hasCommented =
                commentRepo.existsByAccount_IdAndProduct_Id(
                        account.getId(), productId);

        if (hasCompletedOrder && !hasCommented && product != null) {

            Comment comment = new Comment();
            comment.setAccount(account);
            comment.setProduct(product);
            comment.setContent(content);
            comment.setRating(rating);
            comment.setCreatedAt(LocalDateTime.now());

            if (imageFile != null && !imageFile.isEmpty()) {
                try {

                    String fileName =
                            System.currentTimeMillis()
                                    + "_" + imageFile.getOriginalFilename();

                    Path uploadPath =
                            Paths.get("uploads/comments");

                    if (!Files.exists(uploadPath))
                        Files.createDirectories(uploadPath);

                    Files.write(uploadPath.resolve(fileName),
                            imageFile.getBytes());

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