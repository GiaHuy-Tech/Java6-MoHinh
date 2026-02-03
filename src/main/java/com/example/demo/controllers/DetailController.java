package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
import com.example.demo.model.Voucher;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.OrdersDetailRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.VoucherRepository;

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
    private VoucherRepository voucherRepo;
    @Autowired
    private HttpSession session;

    // ================== PRODUCT DETAIL ==================
    @GetMapping("/product-detail/{id}")
    public String productDetail(
            @PathVariable Integer id, 
            @RequestParam(required = false) Integer voucherId,
            Model model) {

        // 1. Lấy thông tin Account và Product
        Account account = (Account) session.getAttribute("account");
        Products product = productRepo.findById(id).orElse(null);

        // Nếu không tìm thấy sản phẩm, quay về trang danh sách
        if (product == null) {
            return "redirect:/products";
        }

        // --- 2. Xử lý danh sách Voucher (CHỈ LẤY VOUCHER CỦA USER) ---
        List<Voucher> vouchers = new ArrayList<>();
        if (account != null) {
            // Gọi hàm tìm kiếm voucher của user, đang active và chưa hết hạn
            // (Đảm bảo bạn đã thêm hàm này vào VoucherRepository như hướng dẫn trước)
            try {
                vouchers = voucherRepo.findByAccount_IdAndActiveTrueAndExpiredAtAfter(
                        account.getId(), 
                        LocalDateTime.now()
                );
            } catch (Exception e) {
                // Fallback nếu chưa update Repository: trả về rỗng để tránh lỗi
                System.err.println("Lỗi lấy voucher: " + e.getMessage());
            }
        }
        // Đẩy danh sách voucher của user sang view để hiển thị trong Dropdown
        model.addAttribute("vouchers", vouchers);


        // --- 3. Tính toán giá tiền & Validate Voucher được chọn ---
        double finalPrice = product.getPrice();
        Voucher selectedVoucher = null;
        String voucherError = null;

        // Chỉ xử lý voucher nếu có ID gửi lên VÀ user đã đăng nhập
        if (voucherId != null && account != null) {
            
            // Bước quan trọng: Kiểm tra xem voucherId này có nằm trong danh sách voucher CỦA USER không?
            // (Tránh trường hợp user nhập bừa ID của voucher người khác)
            boolean isOwner = vouchers.stream().anyMatch(v -> v.getId().equals(voucherId));

            if (isOwner) {
                selectedVoucher = voucherRepo.findById(voucherId).orElse(null);

                if (selectedVoucher != null) {
                    // Logic check điều kiện voucher
                    if (selectedVoucher.getMinOrderValue() != null && product.getPrice() < selectedVoucher.getMinOrderValue()) {
                        voucherError = "Đơn hàng chưa đạt giá trị tối thiểu: " 
                                     + String.format("%,.0f", selectedVoucher.getMinOrderValue()) + "đ";
                        selectedVoucher = null; // Hủy chọn
                    } 
                    else {
                        // HỢP LỆ -> Tính giá giảm
                        if (selectedVoucher.getDiscountPercent() != null) {
                            // Giảm theo %
                            double discountAmount = product.getPrice() * (selectedVoucher.getDiscountPercent() / 100.0);
                            finalPrice -= discountAmount;
                        } else if (selectedVoucher.getDiscountAmount() != null) {
                            // Giảm tiền mặt
                            finalPrice -= selectedVoucher.getDiscountAmount();
                        }
                    }
                }
            } else {
                voucherError = "Bạn không sở hữu mã giảm giá này.";
            }
        }

        // Đảm bảo giá không âm
        if (finalPrice < 0) finalPrice = 0;


        // --- 4. Kiểm tra quyền Comment (Đã mua hàng chưa?) ---
        boolean canComment = false;
        if (account != null) {
            boolean purchased = orderDetailRepo.hasPurchased(account.getId(), id);
            boolean completed = orderDetailRepo.hasCompletedOrder(account.getId(), id);
            boolean commented = commentRepo.existsByAccount_IdAndProduct_Id(account.getId(), id);
            
            // Điều kiện: Đã mua hoặc hoàn tất đơn AND chưa comment bao giờ
            canComment = (purchased || completed) && !commented;
        }

        // --- 5. Đẩy dữ liệu ra View ---
        model.addAttribute("product", product);
        model.addAttribute("canComment", canComment);
        model.addAttribute("finalPrice", finalPrice);
        model.addAttribute("selectedVoucher", selectedVoucher);
        model.addAttribute("voucherError", voucherError);
        
        // Lấy danh sách comment mới nhất
        model.addAttribute("comments", commentRepo.findByProduct_IdOrderByCreatedAtDesc(id));

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
        
        // Chưa đăng nhập -> đá về trang login
        if (account == null) {
            return "redirect:/login";
        }

        Products product = productRepo.findById(productId).orElse(null);
        if(product == null) {
            return "redirect:/products";
        }

        // Tạo đối tượng Comment
        Comment comment = new Comment();
        comment.setAccount(account);
        comment.setProduct(product);
        comment.setContent(content);
        comment.setRating(rating);
        comment.setCreatedAt(LocalDateTime.now());

        // Xử lý upload ảnh comment (nếu có)
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                // Đặt tên file unique để tránh trùng lặp
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                
                // Đường dẫn lưu file: uploads/comments
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
        
        // Redirect lại trang chi tiết để thấy comment vừa đăng
        return "redirect:/product-detail/" + productId;
    }
}