package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
import com.example.demo.model.Cart;
import com.example.demo.model.CartDetail;
import com.example.demo.model.Comment;
import com.example.demo.model.Products;
import com.example.demo.model.ProductImage; // <--- Import Model Ảnh
import com.example.demo.model.Voucher;
import com.example.demo.repository.CartDetailRepository;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.OrdersDetailRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductImageRepository; // <--- Import Repo Ảnh
import com.example.demo.repository.VoucherRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class DetailController {

    @Autowired
    private ProductRepository productRepo;
    
    @Autowired
    private ProductImageRepository productImageRepo; // <--- Inject Repository Ảnh vào đây
    
    @Autowired
    private CommentRepository commentRepo;
    @Autowired
    private OrdersDetailRepository orderDetailRepo;
    @Autowired
    private VoucherRepository voucherRepo;
    @Autowired
    private CartRepository cartRepo;
    @Autowired
    private CartDetailRepository cartDetailRepo;
    @Autowired
    private HttpSession session;

    // ================== PRODUCT DETAIL ==================
    @GetMapping("/product-detail/{id}")
    public String productDetail(@PathVariable Integer id, @RequestParam(required = false) Integer voucherId, Model model) {
        Account account = (Account) session.getAttribute("account");
        Products product = productRepo.findById(id).orElse(null);

        if (product == null) return "redirect:/products";

        // --- 1. LẤY DANH SÁCH ẢNH PHỤ (Logic Mới) ---
        // Lấy list ảnh từ bảng products_image dựa vào product_id
        List<ProductImage> extraImages = productImageRepo.findByProduct_Id(id);
        model.addAttribute("extraImages", extraImages);

        // --- 2. Voucher logic ---
        List<Voucher> vouchers = new ArrayList<>();
        if (account != null) {
            try {
                vouchers = voucherRepo.findByAccount_IdAndActiveTrueAndExpiredAtAfter(account.getId(), LocalDateTime.now());
            } catch (Exception e) {}
        }
        model.addAttribute("vouchers", vouchers);

        // --- 3. Price logic ---
        double finalPrice = product.getPrice();
        Voucher selectedVoucher = null;
        String voucherError = null;

        if (voucherId != null && account != null) {
            boolean isOwner = vouchers.stream().anyMatch(v -> v.getId().equals(voucherId));
            if (isOwner) {
                selectedVoucher = voucherRepo.findById(voucherId).orElse(null);
                if (selectedVoucher != null) {
                    if (selectedVoucher.getMinOrderValue() != null && product.getPrice() < selectedVoucher.getMinOrderValue()) {
                        voucherError = "Chưa đạt giá trị tối thiểu.";
                        selectedVoucher = null;
                    } else {
                        if (selectedVoucher.getDiscountPercent() != null) {
                            finalPrice -= product.getPrice() * (selectedVoucher.getDiscountPercent() / 100.0);
                        } else if (selectedVoucher.getDiscountAmount() != null) {
                            finalPrice -= selectedVoucher.getDiscountAmount();
                        }
                    }
                }
            } else {
                voucherError = "Mã không hợp lệ.";
            }
        }
        if (finalPrice < 0) finalPrice = 0;

        // --- 4. Comment Permission Logic ---
        boolean canComment = false;
        if (account != null) {
            boolean hasCompletedOrder = orderDetailRepo.hasCompletedOrder(account.getId(), id);
            boolean hasCommented = commentRepo.existsByAccount_IdAndProduct_Id(account.getId(), id);
            canComment = hasCompletedOrder && !hasCommented;
        }

        model.addAttribute("product", product);
        model.addAttribute("canComment", canComment);
        model.addAttribute("finalPrice", finalPrice);
        model.addAttribute("selectedVoucher", selectedVoucher);
        model.addAttribute("voucherError", voucherError);
        model.addAttribute("comments", commentRepo.findByProduct_IdOrderByCreatedAtDesc(id));

        return "client/product-detail";
    }

    // ================== ADD TO CART ==================
    @PostMapping("/cart/add")
    public String addToCart(
            @RequestParam Integer productId,
            @RequestParam Integer quantity,
            RedirectAttributes redirectAttributes) {

        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        Cart cart = cartRepo.findByAccount(account).orElse(null);
        if (cart == null) {
            cart = new Cart();
            cart.setAccount(account);
            cart.setCreatedDate(new Date());
            cartRepo.save(cart);
        }

        Products product = productRepo.findById(productId).orElse(null);
        if (product != null) {
            Optional<CartDetail> existingDetail = cartDetailRepo.findByCartAndProduct(cart, product);

            if (existingDetail.isPresent()) {
                CartDetail cartDetail = existingDetail.get();
                cartDetail.setQuantity(cartDetail.getQuantity() + quantity);
                cartDetailRepo.save(cartDetail);
            } else {
                CartDetail newDetail = new CartDetail();
                newDetail.setCart(cart);
                newDetail.setProduct(product);
                newDetail.setQuantity(quantity);
                newDetail.setPrice((int) product.getPrice());
                cartDetailRepo.save(newDetail);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm vào giỏ hàng!");
        }

        return "redirect:/cart"; 
    }

    // ================== POST COMMENT ==================
    @PostMapping("/product-detail/comment/{productId}")
    public String postComment(
            @PathVariable Integer productId,
            @RequestParam String content,
            @RequestParam Integer rating,
            @RequestParam(required = false) MultipartFile imageFile) {
            
        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        Products product = productRepo.findById(productId).orElse(null);
        
        boolean hasCompletedOrder = orderDetailRepo.hasCompletedOrder(account.getId(), productId);
        boolean hasCommented = commentRepo.existsByAccount_IdAndProduct_Id(account.getId(), productId);

        if (hasCompletedOrder && !hasCommented && product != null) {
            Comment comment = new Comment();
            comment.setAccount(account);
            comment.setProduct(product);
            comment.setContent(content);
            comment.setRating(rating);
            comment.setCreatedAt(LocalDateTime.now());

            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                    Path uploadPath = Paths.get("uploads/comments");
                    if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
                    Files.write(uploadPath.resolve(fileName), imageFile.getBytes());
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