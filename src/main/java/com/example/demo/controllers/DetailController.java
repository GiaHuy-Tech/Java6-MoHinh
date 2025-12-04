package com.example.demo.controllers;

import java.time.LocalDateTime;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.demo.model.*;
import com.example.demo.repository.*;

import jakarta.servlet.http.HttpSession;

@Controller
public class DetailController {

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private CommentRepository commentRepo;

    @GetMapping("/product-detail/{id}")
    public String productDetail(@PathVariable Integer id, Model model) {
        Products product = productRepo.findById(id).orElse(null);
        model.addAttribute("product", product);
        model.addAttribute("comments", commentRepo.findByProduct_IdOrderByCreatedDateDesc(id));
        return "client/product-detail";
    }

    @PostMapping("/product-detail/{id}/comment")
    public String postComment(@PathVariable Integer id,
                              @RequestParam("content") String content,
                              HttpSession session) {
        Account acc = (Account) session.getAttribute("account");
        if (acc == null || content.trim().isEmpty()) {
            return "redirect:/login";
        }

        Products product = productRepo.findById(id).orElse(null);
        if (product == null) return "redirect:/products";

        Comment cmt = new Comment();
        cmt.setContent(content);
        cmt.setCreatedDate(LocalDateTime.now()); // dùng cho hiển thị
        cmt.setCreatedAt(LocalDateTime.now());   
        cmt.setProduct(product);
        cmt.setAccount(acc);

        commentRepo.save(cmt);

        return "redirect:/product-detail/" + id;
    }

}