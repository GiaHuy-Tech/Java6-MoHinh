package com.example.demo.controllers;

<<<<<<< Updated upstream
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
=======
import java.math.BigDecimal;
import java.util.List;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
>>>>>>> Stashed changes
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.GhnShippingService;
import com.example.demo.service.MembershipService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired private HttpSession session;
    @Autowired private CartDetailRepository cartDetailRepo;
    @Autowired private OrdersRepository orderRepo;
    @Autowired private OrdersDetailRepository orderDetailRepo;
    @Autowired private AccountRepository accountRepo;

    @Autowired private MembershipService membershipService;
    @Autowired private GhnShippingService ghnShippingService;

<<<<<<< Updated upstream
    // ===================== VIEW CHECKOUT =====================
    @GetMapping
    public String viewCheckout(Model model) {
=======
    @Autowired
    private OrdersDetailRepository orderDetailRepo;
>>>>>>> Stashed changes

        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

<<<<<<< Updated upstream
        List<CartDetail> cartDetails =
                cartDetailRepo.findByCart_Account_Id(account.getId());

        if (cartDetails.isEmpty()) return "redirect:/cart";

        // 1️⃣ Tổng tiền hàng
        int subTotal = cartDetails.stream()
                .mapToInt(cd -> cd.getPrice() * cd.getQuantity())
                .sum();

        // 2️⃣ TÍNH PHÍ SHIP GHN
        int shippingFee = ghnShippingService.tinhPhiTuCart(account, cartDetails);

        // 3️⃣ Giảm giá theo hạng thành viên
        int discountPercent =
                membershipService.getDiscountPercent(account.getMembershipLevel());
        int discountAmount = subTotal * discountPercent / 100;

        // 4️⃣ Tổng cuối
        int total = subTotal - discountAmount + shippingFee;

        model.addAttribute("cartDetails", cartDetails);
        model.addAttribute("subTotal", subTotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("discountPercent", discountPercent);
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("total", total);
        model.addAttribute("account", account);

        return "client/checkout";
=======
    @PostMapping("/{accountId}")
    @Transactional 
    public String checkout(@PathVariable Integer accountId) {

        Account account = accountRepo.findById(accountId).orElse(null);
        if (account == null) {
            return "redirect:/auth/login";
        }

        List<CartDetail> cartList = cartDetailRepo.findByAccount_Id(accountId);
        if (cartList == null || cartList.isEmpty()) {
            return "redirect:/cart/" + accountId;
        }

        // Dùng BigDecimal để tính toán tổng tiền chính xác
        BigDecimal totalCal = BigDecimal.ZERO;

        for (CartDetail item : cartList) {
            if (item.getProduct() != null) { 
                // ✅ Sửa lỗi Type mismatch (Ảnh 1)
                BigDecimal price = BigDecimal.valueOf(item.getProduct().getPrice());
                BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
                totalCal = totalCal.add(price.multiply(quantity));
            }
        }

        // ===== TẠO ORDER =====
        Orders order = new Orders();
        order.setAccount(account);
        order.setCreatedDate(new Date()); 
        
        // Gán giá trị vào Double (Model Orders dùng Double)
        order.setTotal(totalCal.doubleValue());
        order.setFeeship(0.0); 
        order.setStatus(0);
        order.setPaymentStatus(false);

        Orders savedOrder = ordersRepo.save(order);

        // ===== TẠO ORDER DETAIL =====
        for (CartDetail item : cartList) {
            if (item.getProduct() != null) {
                OrderDetail detail = new OrderDetail();
                
                // ✅ Sửa lỗi undefined method setOrders (Ảnh 2)
                // Đổi thành setOrder (theo đúng field trong Model OrderDetail của bạn)
                detail.setOrder(savedOrder); 
                
                detail.setProduct(item.getProduct());
                detail.setQuantity(item.getQuantity());
                
                // Ép kiểu về Double cho đúng Model OrderDetail
                detail.setPrice((double) item.getProduct().getPrice());
                
                orderDetailRepo.save(detail);
            }
        }

        // ===== CẬP NHẬT TỔNG CHI TIÊU ACCOUNT =====
        BigDecimal currentSpending = (account.getTotalSpending() != null) 
                                     ? account.getTotalSpending() : BigDecimal.ZERO;
        account.setTotalSpending(currentSpending.add(totalCal));
        accountRepo.save(account);

        // ===== XOÁ GIỎ HÀNG =====
        cartDetailRepo.deleteAll(cartList);

        return "redirect:/orders/history/" + accountId;
>>>>>>> Stashed changes
    }

    // ===================== PROCESS CHECKOUT =====================
    @PostMapping
    public String processCheckout(
            @RequestParam String address,
            @RequestParam String phone,
            @RequestParam String paymentMethod) {

        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        account = accountRepo.findById(account.getId()).orElse(null);
        if (account == null) return "redirect:/login";

        List<CartDetail> cartDetails =
                cartDetailRepo.findByCart_Account_Id(account.getId());

        if (cartDetails.isEmpty()) return "redirect:/cart";

        // 1️⃣ Tổng tiền hàng
        int subTotal = cartDetails.stream()
                .mapToInt(cd -> cd.getPrice() * cd.getQuantity())
                .sum();

        // 2️⃣ TÍNH SHIP GHN (SERVER-SIDE – KHÔNG TIN FRONTEND)
        int shippingFee = ghnShippingService.tinhPhiTuCart(account, cartDetails);

        // 3️⃣ Giảm giá thành viên
        int discountPercent =
                membershipService.getDiscountPercent(account.getMembershipLevel());
        int discountAmount = subTotal * discountPercent / 100;

        int finalTotal = subTotal - discountAmount + shippingFee;

        // ===================== LƯU ĐƠN HÀNG =====================
        Orders order = new Orders();
        order.setAccountId(account);
        order.setCreatedDate(new Date());
        order.setAddress(address);
        order.setPhone(phone);
        order.setPaymentMethod(paymentMethod);
        order.setFeeship(shippingFee);
        order.setTotal(finalTotal);
        order.setPaymentStatus(false);
        order.setStatus(0);

        String orderCode = "DH" + System.currentTimeMillis();
        order.setNote(orderCode);

        Orders savedOrder = orderRepo.save(order);

        // ===================== LƯU CHI TIẾT =====================
        List<OrderDetail> orderDetails = cartDetails.stream().map(cd -> {
            OrderDetail od = new OrderDetail();
            od.setOrders(savedOrder);
            od.setProductId(cd.getProduct());
            od.setQuantity(cd.getQuantity());
            od.setPrice(cd.getPrice());
            return od;
        }).toList();

        orderDetailRepo.saveAll(orderDetails);
        cartDetailRepo.deleteAll(cartDetails);

        // ===================== THANH TOÁN =====================
        if ("COD".equals(paymentMethod)) {
            updateMembershipSpending(account, finalTotal);
            return "redirect:/orders";
        }

        session.setAttribute("pendingOrderId", savedOrder.getId());
        return "redirect:/checkout/payment";
    }

    // ===================== VIEW QR PAYMENT =====================
    @GetMapping("/payment")
    public String viewPaymentQR(Model model) {

        Integer orderId = (Integer) session.getAttribute("pendingOrderId");
        if (orderId == null) return "redirect:/cart";

        Orders order = orderRepo.findById(orderId).orElse(null);
        if (order == null) return "redirect:/cart";

        if (Boolean.TRUE.equals(order.getPaymentStatus()))
            return "redirect:/orders";

        String bankId = "ICB";
        String accountNo = "103878028110";
        String accountName = "NGUYEN GIA HUY";

        String qrUrl = "";
        if ("VIETQR".equals(order.getPaymentMethod())) {
            qrUrl = String.format(
                    "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                    bankId,
                    accountNo,
                    order.getTotal(),
                    order.getNote(),
                    URLEncoder.encode(accountName, StandardCharsets.UTF_8)
            );
        }

        model.addAttribute("qrUrl", qrUrl);
        model.addAttribute("total", order.getTotal());
        model.addAttribute("content", order.getNote());
        model.addAttribute("accountNo", accountNo);
        model.addAttribute("accountName", accountName);

        return "client/payment-qr";
    }

    // ===================== CHECK STATUS (AJAX) =====================
    @GetMapping("/check-status")
    @ResponseBody
    public Map<String, Boolean> checkOrderStatus() {

        Map<String, Boolean> res = new HashMap<>();
        Integer orderId = (Integer) session.getAttribute("pendingOrderId");

        if (orderId != null) {
            Orders order = orderRepo.findById(orderId).orElse(null);
            if (order != null && Boolean.TRUE.equals(order.getPaymentStatus())) {
                res.put("paid", true);
                return res;
            }
        }
        res.put("paid", false);
        return res;
    }

    // ===================== CONFIRM PAYMENT =====================
    @PostMapping("/confirm-payment")
    public String confirmPaymentManual() {

        Integer orderId = (Integer) session.getAttribute("pendingOrderId");
        if (orderId == null) return "redirect:/orders";

        Orders order = orderRepo.findById(orderId).orElse(null);
        if (order != null) {
            order.setPaymentStatus(true);
            order.setStatus(1);
            orderRepo.save(order);

            updateMembershipSpending(order.getAccountId(), order.getTotal());
        }

        session.removeAttribute("pendingOrderId");
        return "redirect:/orders";
    }

    // ===================== UPDATE MEMBERSHIP =====================
    private void updateMembershipSpending(Account account, int amount) {

        long current =
                account.getTotalSpending() == null ? 0 : account.getTotalSpending();

        account.setTotalSpending(current + amount);
        membershipService.updateMembershipLevel(account);

        accountRepo.save(account);
        session.setAttribute("account", account);
    }
}
