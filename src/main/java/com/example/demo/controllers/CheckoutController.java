package com.example.demo.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.*;
import com.example.demo.repository.*;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private HttpSession session;

    @Autowired
    private CartDetailRepository cartDetailRepo;

    @Autowired
    private OrdersRepository orderRepo;

    @Autowired
    private OrdersDetailRepository orderDetailRepo;

    /**
     * ✅ GET: Hiển thị trang thanh toán
     */
    @GetMapping
    public String viewCheckout(Model model) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        List<CartDetail> cartDetails = cartDetailRepo.findByCart_Account_Id(account.getId());
        if (cartDetails.isEmpty()) return "redirect:/cart";

        // Tính tổng tiền hàng
        int subTotal = cartDetails.stream()
                .mapToInt(cd -> cd.getPrice() * cd.getQuantity())
                .sum();

        // Logic phí ship: < 1 triệu thì 5%, >= 1 triệu thì Free
        int shippingFee = (subTotal < 1000000) ? (int)(subTotal * 0.05) : 0;

        model.addAttribute("cartDetails", cartDetails);
        model.addAttribute("total", subTotal); 
        model.addAttribute("shippingFee", shippingFee); 
        model.addAttribute("account", account);

        return "client/checkout";
    }

    /**
     * ✅ POST: Xử lý đặt hàng
     */
    @PostMapping
    public String processCheckout(
            @RequestParam("address") String address,
            @RequestParam("phone") String phone,
            @RequestParam("paymentMethod") String paymentMethod,
            Model model) {

        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        List<CartDetail> cartDetails = cartDetailRepo.findByCart_Account_Id(account.getId());
        if (cartDetails.isEmpty()) return "redirect:/cart";

        // 1. Tính toán lại tiền
        int subTotal = cartDetails.stream()
                .mapToInt(cd -> cd.getPrice() * cd.getQuantity())
                .sum();
        
        int shippingFee = (subTotal < 1000000) ? (int)(subTotal * 0.05) : 0;
        int finalTotal = subTotal + shippingFee;

        // 2. Tạo và lưu Orders
        Orders order = new Orders();
        order.setAccountId(account);
        order.setCreatedDate(new Date());
        order.setAddress(address);
        order.setPhone(phone);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(false); 
        order.setFeeship(shippingFee); 
        order.setTotal(finalTotal);    
        order.setStatus(0);            

        Orders savedOrder = orderRepo.save(order);

        // 3. Lưu OrderDetail
        List<OrderDetail> orderDetails = cartDetails.stream().map(item -> {
            OrderDetail detail = new OrderDetail();
            detail.setOrders(savedOrder);
            detail.setProductId(item.getProduct());
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getPrice());
            return detail;
        }).toList();

        orderDetailRepo.saveAll(orderDetails);

        // 4. Xóa giỏ hàng
        cartDetailRepo.deleteAll(cartDetails);

        // 5. ĐIỀU HƯỚNG
        // Nếu chọn VietQR hoặc MOMO -> Chuyển sang trang quét mã
        if ("VIETQR".equals(paymentMethod) || "MOMO".equals(paymentMethod)) {
            return "redirect:/checkout/payment?orderId=" + savedOrder.getId();
        }

        // Mặc định (COD) -> Về trang danh sách đơn
        return "redirect:/orders";
    }

    /**
     * ✅ GET: Trang hiển thị mã QR (Xử lý cả VietQR và Momo)
     */
    @GetMapping("/payment")
    public String viewPaymentQR(@RequestParam("orderId") Integer orderId, Model model) {
        Optional<Orders> orderOpt = orderRepo.findById(orderId);
        if (orderOpt.isEmpty()) return "redirect:/orders";
        
        Orders order = orderOpt.get();
        String paymentMethod = order.getPaymentMethod();

        // Biến để đẩy ra View
        String qrUrl = "";
        String bankName = "";
        String accountNo = "";
        String accountName = "";
        
        // Nội dung CK: "DH" + Mã đơn hàng
        String content = "DH" + order.getId(); 

        // --- TRƯỜNG HỢP 1: VIETQR (NGÂN HÀNG) ---
        if ("VIETQR".equals(paymentMethod)) {
            String BANK_ID = "ICB"; // VietinBank
            accountNo = "103878028110";
            accountName = "NGUYEN GIA HUY";
            bankName = "VietinBank";

            // Tạo URL VietQR
            qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                BANK_ID, 
                accountNo, 
                order.getTotal(), 
                content, 
                URLEncoder.encode(accountName, StandardCharsets.UTF_8)
            );
        } 
        // --- TRƯỜNG HỢP 2: MOMO ---
        else if ("MOMO".equals(paymentMethod)) {
            // ⚠️ THAY SỐ ĐIỆN THOẠI MOMO CỦA BẠN VÀO ĐÂY
            String MOMO_PHONE = "0914211221"; // Ví dụ sđt momo của bạn
            
            accountNo = MOMO_PHONE;
            accountName = "NGUYEN GIA HUY";
            bankName = "Ví điện tử MoMo";

            // 1. Tạo link chuyển tiền Momo cá nhân
            String momoLink = String.format(
                "https://me.momo.vn/%s?amount=%d&message=%s",
                MOMO_PHONE, 
                order.getTotal(), 
                URLEncoder.encode(content, StandardCharsets.UTF_8)
            );

            // 2. Dùng API tạo QR Code miễn phí để biến Link Momo thành Ảnh QR
            qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=" + URLEncoder.encode(momoLink, StandardCharsets.UTF_8);
        }

        model.addAttribute("qrUrl", qrUrl);
        model.addAttribute("order", order);
        model.addAttribute("bankName", bankName);
        model.addAttribute("accountNo", accountNo);
        model.addAttribute("accountName", accountName);
        
        return "client/payment-qr"; 
    }
}