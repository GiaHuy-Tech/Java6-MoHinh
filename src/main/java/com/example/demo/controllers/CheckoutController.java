package com.example.demo.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.LocalShippingService;
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
    @Autowired private LocalShippingService localShippingService;

    // ===================== VIEW CHECKOUT =====================
    @GetMapping
    public String viewCheckout(Model model) {

        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        List<CartDetail> cartDetails =
                cartDetailRepo.findByCart_Account_Id(account.getId());

        if (cartDetails.isEmpty()) return "redirect:/cart";

        // ===== 1️⃣ Tổng tiền hàng =====
        int subTotal = cartDetails.stream()
                .mapToInt(cd -> cd.getPrice() * cd.getQuantity())
                .sum();

        // ===== 2️⃣ Tổng cân nặng (gram) =====
        int totalWeight = cartDetails.stream()
                .mapToInt(cd -> {
                    Double kg = cd.getProduct().getWeight();
                    if (kg == null) return 0;
                    return (int) Math.ceil(kg * 1000) * cd.getQuantity();
                }).sum();

        // ===== 3️⃣ GIẢ LẬP TỌA ĐỘ KHÁCH (ĐỔI THEO TỈNH) =====
        double customerLat = 10.762622;   // TP.HCM demo
        double customerLng = 106.660172;

        // ===== 4️⃣ TÍNH SHIP =====
        int shippingFee = localShippingService.calculateShipping(
                customerLat,
                customerLng,
                totalWeight,
                subTotal,
                false
        );

        // ===== 5️⃣ Giảm giá thành viên =====
        int discountPercent =
                membershipService.getDiscountPercent(account.getMembershipLevel());
        int discountAmount = subTotal * discountPercent / 100;

        int total = subTotal - discountAmount + shippingFee;

        model.addAttribute("cartDetails", cartDetails);
        model.addAttribute("subTotal", subTotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("discountPercent", discountPercent);
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("total", total);
        model.addAttribute("account", account);

        return "client/checkout";
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

        // ===== 1️⃣ Tổng tiền =====
        int subTotal = cartDetails.stream()
                .mapToInt(cd -> cd.getPrice() * cd.getQuantity())
                .sum();

        // ===== 2️⃣ Tổng cân nặng =====
        int totalWeight = cartDetails.stream()
                .mapToInt(cd -> {
                    Double kg = cd.getProduct().getWeight();
                    if (kg == null) return 0;
                    return (int) Math.ceil(kg * 1000) * cd.getQuantity();
                }).sum();

        // ===== 3️⃣ Tọa độ khách =====
        double customerLat = 10.762622;
        double customerLng = 106.660172;

        boolean isCOD = "COD".equals(paymentMethod);

        int shippingFee = localShippingService.calculateShipping(
                customerLat,
                customerLng,
                totalWeight,
                subTotal,
                isCOD
        );

        // ===== 4️⃣ Giảm giá =====
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

        // ===== COD =====
        if (isCOD) {
            updateMembershipSpending(account, finalTotal);
            return "redirect:/orders";
        }

        session.setAttribute("pendingOrderId", savedOrder.getId());
        return "redirect:/checkout/payment";
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