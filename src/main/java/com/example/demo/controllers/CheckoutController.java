package com.example.demo.controllers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ===== VOUCHER =====
        List<VoucherDetail> myVoucherDetails =
                voucherDetailRepo.findByAccount_IdAndIsUsedFalse(account.getId());

        List<Voucher> publicVouchers = voucherRepo.findByAccountIsNull();

        List<Voucher> availableVouchers = new ArrayList<>();
        myVoucherDetails.forEach(vd -> availableVouchers.add(vd.getVoucher()));
        availableVouchers.addAll(publicVouchers);

        LocalDateTime now = LocalDateTime.now();

        List<Voucher> savedVouchers = availableVouchers.stream()
                .filter(v -> v != null && Boolean.TRUE.equals(v.getActive()))
                .filter(v -> v.getExpiredAt() == null || v.getExpiredAt().isAfter(now))
                .filter(v -> v.getMinOrderValue() == null
                        || rawTotal.doubleValue() >= v.getMinOrderValue())
                .distinct()
                .collect(Collectors.toList());

        // ===== APPLY VOUCHER =====
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal feeShip = BigDecimal.valueOf(30000);

        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            Optional<Voucher> vOpt = savedVouchers.stream()
                    .filter(v -> v.getCode().equals(voucherCode.trim()))
                    .findFirst();

            if (vOpt.isPresent()) {
                Voucher v = vOpt.get();

                if (v.getDiscountPercent() != null) {
                    discount = rawTotal.multiply(
                            BigDecimal.valueOf(v.getDiscountPercent())
                                    .divide(BigDecimal.valueOf(100)));
                } else if (v.getDiscountAmount() != null) {
                    discount = BigDecimal.valueOf(v.getDiscountAmount());
                }

                if (discount.compareTo(rawTotal) > 0) discount = rawTotal;
                if (Boolean.TRUE.equals(v.getIsFreeShipping())) feeShip = BigDecimal.ZERO;
            }
        }

        BigDecimal finalTotal = rawTotal.subtract(discount).add(feeShip);

        // 🔥 FIX CHUẨN: dùng method mới
        List<Address> addresses = addressRepo.findByAccount_Id(account.getId());

        model.addAttribute("cartList", cartList);
        model.addAttribute("addresses", addresses);
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
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal feeShip = BigDecimal.valueOf(30000);

        // ===== VOUCHER =====
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            Optional<Voucher> vOpt = voucherRepo.findAll().stream()
                    .filter(v -> v.getCode().equals(voucherCode.trim())
                            && Boolean.TRUE.equals(v.getActive()))
                    .findFirst();

            if (vOpt.isPresent()) {
                Voucher v = vOpt.get();

                if (v.getDiscountPercent() != null) {
                    discount = rawTotal.multiply(
                            BigDecimal.valueOf(v.getDiscountPercent())
                                    .divide(BigDecimal.valueOf(100)));
                } else if (v.getDiscountAmount() != null) {
                    discount = BigDecimal.valueOf(v.getDiscountAmount());
                }

                if (discount.compareTo(rawTotal) > 0) discount = rawTotal;
                if (Boolean.TRUE.equals(v.getIsFreeShipping())) feeShip = BigDecimal.ZERO;

                // update voucher detail
                voucherDetailRepo.findValidVoucherForAccount(account.getId(), voucherCode.trim())
                        .ifPresent(vd -> {
                            vd.setIsUsed(true);
                            vd.setUsedAt(new Date());
                            vd.setStatus("USED");
                            voucherDetailRepo.save(vd);
                        });
            }
        }

        BigDecimal finalTotal = rawTotal.subtract(discount).add(feeShip);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        // 🔥 FIX CHUẨN BẢO MẬT
        Address selectedAddress = addressRepo
                .findByIdAndAccount_Id(addressId, account.getId())
                .orElse(null);

        // ===== CREATE ORDER =====
        Orders order = new Orders();
        order.setAccount(account);
        order.setCreatedDate(new Date());
        order.setAddress(selectedAddress);

        if (selectedAddress != null) {
            order.setPhone(selectedAddress.getRecipientPhone());
        }

        order.setTotal(finalTotal);
        order.setFeeship(feeShip);
        order.setMoneyDiscounted(discount);
        order.setStatus(0);
        order.setPaymentStatus(false);
        order.setVoucherCode(voucherCode);
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
            long amount = finalTotal.longValue() * 100;

            String paymentUrl = vnPayService.createOrder(
                    (int) amount,
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