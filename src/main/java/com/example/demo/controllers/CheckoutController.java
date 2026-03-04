package com.example.demo.controllers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private CartDetailRepository cartDetailRepo;

    @Autowired
    private OrdersRepository ordersRepo;

    @Autowired
    private OrdersDetailRepository orderDetailRepo;

    @Autowired
    private VoucherDetailRepository voucherDetailRepo;

    @Autowired
    private AccountRepository accountRepo;

    // =====================================================
    // 1️⃣ HIỂN THỊ TRANG CHECKOUT + TÍNH VOUCHER
    // =====================================================
    @GetMapping
    public String viewCheckout(HttpSession session,
                               @RequestParam(required = false) String voucherCode,
                               Model model) {

        Account account = getAccount(session);
        if (account == null) return "redirect:/login";

        List<CartDetail> cartList =
                cartDetailRepo.findByAccount_Id(account.getId());

        if (cartList.isEmpty())
            return "redirect:/cart";

        // ===== TÍNH TIỀN GỐC =====
        BigDecimal rawTotal = BigDecimal.ZERO;

        for (CartDetail item : cartList) {
            rawTotal = rawTotal.add(
                    item.getProduct().getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()))
            );
        }

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal feeShip = BigDecimal.ZERO;
        VoucherDetail appliedVoucherDetail = null;

        // ===== XỬ LÝ VOUCHER =====
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {

            Optional<VoucherDetail> voucherOpt =
                    voucherDetailRepo.findValidVoucherForAccount(
                            account.getId(),
                            voucherCode.trim()
                    );

            if (voucherOpt.isPresent()) {

                VoucherDetail vd = voucherOpt.get();
                Voucher v = vd.getVoucher();

                boolean notExpired =
                        v.getExpiredAt() == null ||
                        v.getExpiredAt().isAfter(LocalDateTime.now());

                boolean meetMin =
                        v.getMinOrderValue() == null ||
                        rawTotal.doubleValue() >= v.getMinOrderValue();

                if (notExpired && meetMin) {

                    appliedVoucherDetail = vd;

                    if (v.getDiscountPercent() != null
                            && v.getDiscountPercent() > 0) {

                        discount = rawTotal.multiply(
                                BigDecimal.valueOf(v.getDiscountPercent())
                                        .divide(BigDecimal.valueOf(100))
                        );

                    } else if (v.getDiscountAmount() != null
                            && v.getDiscountAmount() > 0) {

                        discount = BigDecimal.valueOf(v.getDiscountAmount());
                    }

                    if (discount.compareTo(rawTotal) > 0)
                        discount = rawTotal;

                    if (Boolean.TRUE.equals(v.getIsFreeShipping()))
                        feeShip = BigDecimal.ZERO;
                }
            }
        }

        BigDecimal finalTotal = rawTotal.subtract(discount).add(feeShip);

        if (finalTotal.compareTo(BigDecimal.ZERO) < 0)
            finalTotal = BigDecimal.ZERO;

        model.addAttribute("cartList", cartList);
        model.addAttribute("rawTotal", rawTotal);
        model.addAttribute("discount", discount);
        model.addAttribute("feeShip", feeShip);
        model.addAttribute("finalTotal", finalTotal);
        model.addAttribute("voucherCode", voucherCode);

        return "client/checkout";
    }

    // =====================================================
    // 2️⃣ CONFIRM ĐƠN HÀNG (TẠO ORDER)
    // =====================================================
    @PostMapping("/confirm")
    public String confirmOrder(HttpSession session,
                               @RequestParam(required = false) String voucherCode) {

        Account account = getAccount(session);
        if (account == null) return "redirect:/login";

        List<CartDetail> cartList =
                cartDetailRepo.findByAccount_Id(account.getId());

        if (cartList.isEmpty())
            return "redirect:/cart";

        // ===== TÍNH LẠI TIỀN (BẮT BUỘC) =====
        BigDecimal rawTotal = BigDecimal.ZERO;

        for (CartDetail item : cartList) {
            rawTotal = rawTotal.add(
                    item.getProduct().getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()))
            );
        }

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal feeShip = BigDecimal.ZERO;
        VoucherDetail appliedVoucherDetail = null;

        if (voucherCode != null && !voucherCode.trim().isEmpty()) {

            Optional<VoucherDetail> voucherOpt =
                    voucherDetailRepo.findValidVoucherForAccount(
                            account.getId(),
                            voucherCode.trim()
                    );

            if (voucherOpt.isPresent()) {
                VoucherDetail vd = voucherOpt.get();
                Voucher v = vd.getVoucher();

                appliedVoucherDetail = vd;

                if (v.getDiscountPercent() != null) {
                    discount = rawTotal.multiply(
                            BigDecimal.valueOf(v.getDiscountPercent())
                                    .divide(BigDecimal.valueOf(100))
                    );
                } else if (v.getDiscountAmount() != null) {
                    discount = BigDecimal.valueOf(v.getDiscountAmount());
                }

                if (discount.compareTo(rawTotal) > 0)
                    discount = rawTotal;

                if (Boolean.TRUE.equals(v.getIsFreeShipping()))
                    feeShip = BigDecimal.ZERO;
            }
        }

        BigDecimal finalTotal = rawTotal.subtract(discount).add(feeShip);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0)
            finalTotal = BigDecimal.ZERO;

        // ===== TẠO ORDER =====
        Orders order = new Orders();
        order.setAccount(account);
        order.setCreatedDate(new Date());
        order.setTotal(finalTotal);
        order.setFeeship(feeShip);
        order.setMoneyDiscounted(discount);
        order.setStatus(0);
        order.setPaymentStatus(false);

        if (appliedVoucherDetail != null) {
            order.setVoucherCode(
                    appliedVoucherDetail.getVoucher().getCode()
            );
        }

        ordersRepo.save(order);

        // ===== TẠO ORDER DETAIL =====
        for (CartDetail item : cartList) {

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(item.getProduct());
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getProduct().getPrice());

            orderDetailRepo.save(detail);
        }

        // ===== UPDATE VOUCHER =====
        if (appliedVoucherDetail != null) {
            appliedVoucherDetail.setIsUsed(true);
            appliedVoucherDetail.setUsedAt(new Date());
            appliedVoucherDetail.setStatus("USED");
            voucherDetailRepo.save(appliedVoucherDetail);
        }

        // ===== UPDATE TOTAL SPENDING =====
        BigDecimal current =
                account.getTotalSpending() == null
                        ? BigDecimal.ZERO
                        : account.getTotalSpending();

        account.setTotalSpending(current.add(finalTotal));
        accountRepo.save(account);

        // ===== XOÁ GIỎ HÀNG =====
        cartDetailRepo.deleteAll(cartList);

        return "redirect:/orders";
    }

    // =====================================================
    // LẤY ACCOUNT TỪ SESSION
    // =====================================================
    private Account getAccount(HttpSession session) {
        Account account = (Account) session.getAttribute("account");
        if (account == null)
            account = (Account) session.getAttribute("user");
        return account;
    }
}