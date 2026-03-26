package com.example.demo.controllers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.config.VNPayConfig;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.VNPayService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired private CartDetailRepository cartDetailRepo;
    @Autowired private OrdersRepository ordersRepo;
    @Autowired private OrdersDetailRepository orderDetailRepo;
    @Autowired private VoucherDetailRepository voucherDetailRepo;
    @Autowired private VoucherRepository voucherRepo;
    @Autowired private AccountRepository accountRepo;
    @Autowired private AddressRepository addressRepo;
    @Autowired private VNPayService vnPayService;

    // ================== VIEW ==================
    @GetMapping
    public String viewCheckout(HttpSession session,
                               @RequestParam(required = false) String voucherCode,
                               Model model) {
        Account account = getAccount(session);
        if (account == null) return "redirect:/login";

        List<CartDetail> cartList = cartDetailRepo.findByAccount_Id(account.getId());
        if (cartList.isEmpty()) return "redirect:/cart";

        BigDecimal rawTotal = cartList.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<VoucherDetail> myVoucherDetails = voucherDetailRepo.findByAccount_IdAndIsUsedFalse(account.getId());
        List<Voucher> publicVouchers = voucherRepo.findByAccountIsNull();

        List<Voucher> availableVouchers = new ArrayList<>();
        myVoucherDetails.forEach(vd -> availableVouchers.add(vd.getVoucher()));
        availableVouchers.addAll(publicVouchers);

        List<Voucher> savedVouchers = availableVouchers.stream()
                .filter(v -> v != null && Boolean.TRUE.equals(v.getActive()))
                .filter(v -> v.getMinOrderValue() == null || rawTotal.doubleValue() >= v.getMinOrderValue())
                .distinct()
                .collect(Collectors.toList());

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal feeShip = BigDecimal.valueOf(30000);

        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            Optional<Voucher> vOpt = savedVouchers.stream()
                    .filter(v -> v.getCode().equals(voucherCode.trim()))
                    .findFirst();

            if (vOpt.isPresent()) {
                Voucher v = vOpt.get();

                if (v.getDiscountPercent() != null) {
                    discount = rawTotal.multiply(BigDecimal.valueOf(v.getDiscountPercent()).divide(BigDecimal.valueOf(100)));
                } else if (v.getDiscountAmount() != null) {
                    discount = BigDecimal.valueOf(v.getDiscountAmount());
                }

                if (discount.compareTo(rawTotal) > 0) discount = rawTotal;
                if (Boolean.TRUE.equals(v.getIsFreeShipping())) feeShip = BigDecimal.ZERO;
            }
        }

        BigDecimal finalTotal = rawTotal.subtract(discount).add(feeShip);

        model.addAttribute("cartList", cartList);
        model.addAttribute("addresses", addressRepo.findByAccountId(account.getId()));
        model.addAttribute("rawTotal", rawTotal);
        model.addAttribute("discount", discount);
        model.addAttribute("feeShip", feeShip);
        model.addAttribute("finalTotal", finalTotal);
        model.addAttribute("voucherCode", voucherCode);
        model.addAttribute("savedVouchers", savedVouchers);
        model.addAttribute("user", account);

        return "client/checkout";
    }

    // ================== CONFIRM ==================
    @PostMapping("/confirm")
    public String confirmOrder(HttpSession session,
                               @RequestParam(required = false) String voucherCode,
                               @RequestParam("addressId") Long addressId,
                               @RequestParam("paymentMethod") String paymentMethod) {

        Account account = getAccount(session);
        if (account == null) return "redirect:/login";

        List<CartDetail> cartList = cartDetailRepo.findByAccount_Id(account.getId());
        if (cartList.isEmpty()) return "redirect:/cart";

        BigDecimal rawTotal = cartList.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal feeShip = BigDecimal.valueOf(30000);

        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            Optional<Voucher> vOpt = voucherRepo.findAll().stream()
                    .filter(v -> v.getCode().equals(voucherCode.trim()) && Boolean.TRUE.equals(v.getActive()))
                    .findFirst();

            if (vOpt.isPresent()) {
                Voucher v = vOpt.get();

                if (v.getDiscountPercent() != null) {
                    discount = rawTotal.multiply(BigDecimal.valueOf(v.getDiscountPercent()).divide(BigDecimal.valueOf(100)));
                } else if (v.getDiscountAmount() != null) {
                    discount = BigDecimal.valueOf(v.getDiscountAmount());
                }

                if (discount.compareTo(rawTotal) > 0) discount = rawTotal;
                if (Boolean.TRUE.equals(v.getIsFreeShipping())) feeShip = BigDecimal.ZERO;
            }
        }

        BigDecimal finalTotal = rawTotal.subtract(discount).add(feeShip);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) finalTotal = BigDecimal.ZERO;

        // ===== CREATE ORDER =====
        Orders order = new Orders();
        order.setAccount(account);
        order.setCreatedDate(new Date());
        order.setTotal(finalTotal);
        order.setFeeship(feeShip);
        order.setMoneyDiscounted(discount);
        order.setStatus(0);
        order.setPaymentStatus(false);
        order.setVoucherCode(voucherCode);

        // 🔥 FIX CHÍNH
        order.setPaymentMethod(paymentMethod);

        ordersRepo.save(order);

        // ===== ORDER DETAIL =====
        for (CartDetail item : cartList) {
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(item.getProduct());
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getProduct().getPrice());
            orderDetailRepo.save(detail);
        }

        cartDetailRepo.deleteAll(cartList);

        // ===== PAYMENT =====
        if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            String paymentUrl = vnPayService.createOrder(
                    finalTotal.intValue(),
                    String.valueOf(order.getId()),
                    VNPayConfig.vnp_ReturnUrl
            );
            return "redirect:" + paymentUrl;
        }

        return "redirect:/orders";
    }

    // ================== VNPAY RETURN ==================
    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request) {
        int paymentStatus = vnPayService.orderReturn(request);
        String orderIdStr = request.getParameter("vnp_OrderInfo");

        if (orderIdStr != null) {
            Orders order = ordersRepo.findById(Integer.valueOf(orderIdStr)).orElse(null);

            if (order != null && paymentStatus == 1) {
                order.setPaymentStatus(true);
                order.setStatus(1);

                // 🔥 FIX LUÔN
                order.setPaymentMethod("VNPAY");

                ordersRepo.save(order);
                return "redirect:/orders?success";
            }
        }

        return "redirect:/cart?error";
    }

    // ================== GET ACCOUNT ==================
    private Account getAccount(HttpSession session) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) account = (Account) session.getAttribute("user");
        return account;
    }
}