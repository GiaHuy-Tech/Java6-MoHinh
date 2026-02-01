package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

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

    // ================== PRODUCT DETAIL ==================
    @GetMapping("/product-detail/{id}")
    public String productDetail(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer voucherId,
            Model model) {

        Account currentAccount = (Account) session.getAttribute("account");
        Products product = productRepo.findById(id).orElse(null);
        if (product == null) {
            return "redirect:/products";
        }

        // ===== GIÁ SAU KHI ÁP VOUCHER =====
        double finalPrice = product.getPrice();

        // ===== LẤY DANH SÁCH VOUCHER CÒN HẠN =====
        List<Voucher> vouchers =
                voucherRepo.findByExpiredAtAfterAndActiveTrue(LocalDateTime.now());

        Voucher selectedVoucher = null;

        if (voucherId != null) {
            selectedVoucher = voucherRepo.findById(voucherId).orElse(null);

            if (selectedVoucher != null) {

                // kiểm tra đơn tối thiểu
                if (selectedVoucher.getMinOrderValue() == null
                        || product.getPrice() >= selectedVoucher.getMinOrderValue()) {

                    if (selectedVoucher.getDiscountPercent() != null) {
                        finalPrice = product.getPrice()
                                * (100 - selectedVoucher.getDiscountPercent()) / 100.0;
                    }

                    if (selectedVoucher.getDiscountAmount() != null) {
                        finalPrice = product.getPrice()
                                - selectedVoucher.getDiscountAmount();
                    }

                    if (finalPrice < 0) finalPrice = 0;
                } else {
                    selectedVoucher = null;
                }
            }
        }

        // ===== KIỂM TRA QUYỀN COMMENT =====
        boolean canComment = false;
        if (currentAccount != null) {
            canComment = orderDetailRepo
                    .existsByAccountAndProduct(currentAccount.getId(), id);
        }

        // ===== ĐẨY DATA SANG VIEW =====
        model.addAttribute("product", product);
        model.addAttribute("finalPrice", finalPrice);
        model.addAttribute("vouchers", vouchers);
        model.addAttribute("selectedVoucher", selectedVoucher);
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
            @RequestParam("content") String content,
            @RequestParam("rating") Integer rating,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {

        Account currentAccount = (Account) session.getAttribute("account");
        if (currentAccount == null) {
            return "redirect:/login";
        }

        Products product = productRepo.findById(productId).orElse(null);
        if (product == null) {
            return "redirect:/products";
        }

        Comment comment = new Comment();
        comment.setAccount(currentAccount);
        comment.setProduct(product);
        comment.setContent(content);
        comment.setRating(rating);
        comment.setCreatedAt(LocalDateTime.now());

        // ===== UPLOAD ẢNH COMMENT =====
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String fileName =
                        System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
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

        return "redirect:/product-detail/" + productId;
    }
}
