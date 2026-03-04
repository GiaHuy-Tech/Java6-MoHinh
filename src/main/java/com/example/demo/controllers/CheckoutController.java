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
    private AccountRepository accountRepo;

    @Autowired
    private VoucherDetailRepository voucherDetailRepo;

    // ===============================
    // POST: THANH TOÁN
    // ===============================
    @PostMapping("/{accountId}")
    public String checkout(@PathVariable Integer accountId,
                           @RequestParam(required = false) String voucherCode) {

        Account account = accountRepo.findById(accountId).orElse(null);
        if (account == null) {
            return "redirect:/cart/" + accountId;
        }

        List<CartDetail> cartList = cartDetailRepo.findByAccount_Id(accountId);

        if (cartList.isEmpty()) {
            return "redirect:/cart/" + accountId;
        }

        // ===== 1. TÍNH TIỀN GỐC =====
        BigDecimal rawTotal = BigDecimal.ZERO;

        for (CartDetail item : cartList) {
            BigDecimal price = item.getProduct().getPrice();
            BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
            rawTotal = rawTotal.add(price.multiply(quantity));
        }

        BigDecimal moneyDiscounted = BigDecimal.ZERO;
        BigDecimal feeShip = BigDecimal.ZERO;
        VoucherDetail appliedVoucherDetail = null;

        // ===== 2. XỬ LÝ VOUCHER =====
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {

            Optional<VoucherDetail> voucherOpt =
                    voucherDetailRepo.findValidVoucherForAccount(accountId, voucherCode.trim());

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

                    if (v.getDiscountPercent() != null && v.getDiscountPercent() > 0) {

                        BigDecimal percent =
                                BigDecimal.valueOf(v.getDiscountPercent())
                                .divide(BigDecimal.valueOf(100));

                        moneyDiscounted = rawTotal.multiply(percent);

                    } else if (v.getDiscountAmount() != null && v.getDiscountAmount() > 0) {

                        moneyDiscounted = BigDecimal.valueOf(v.getDiscountAmount());
                    }

                    if (moneyDiscounted.compareTo(rawTotal) > 0) {
                        moneyDiscounted = rawTotal;
                    }

                    if (Boolean.TRUE.equals(v.getIsFreeShipping())) {
                        feeShip = BigDecimal.ZERO;
                    }
                }
            }
        }

        BigDecimal finalTotal = rawTotal.subtract(moneyDiscounted).add(feeShip);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        // ===== 3. TẠO ORDER =====
        Orders order = new Orders();
        order.setAccount(account);
        order.setCreatedDate(new Date());
        order.setTotal(finalTotal);
        order.setFeeship(feeShip);
        order.setMoneyDiscounted(moneyDiscounted);
        order.setStatus(0);
        order.setPaymentStatus(false);

        if (appliedVoucherDetail != null) {
            order.setVoucherCode(appliedVoucherDetail.getVoucher().getCode());
        }

        ordersRepo.save(order);

        // ===== 4. TẠO ORDER DETAIL =====
        for (CartDetail item : cartList) {

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(item.getProduct());
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getProduct().getPrice());

            orderDetailRepo.save(detail);
        }

        // ===== 5. UPDATE VOUCHER =====
        if (appliedVoucherDetail != null) {
            appliedVoucherDetail.setIsUsed(true);
            appliedVoucherDetail.setUsedAt(new Date());
            appliedVoucherDetail.setStatus("USED");
            voucherDetailRepo.save(appliedVoucherDetail);
        }

        // ===== 6. UPDATE TOTAL SPENDING =====
        BigDecimal current =
                account.getTotalSpending() == null
                        ? BigDecimal.ZERO
                        : account.getTotalSpending();

        account.setTotalSpending(current.add(finalTotal));
        accountRepo.save(account);

        // ===== 7. XOÁ GIỎ HÀNG =====
        cartDetailRepo.deleteAll(cartList);

        // ===== 8. REDIRECT SANG ORDERS =====
        return "redirect:/orders/" + accountId;
    }
}