package com.example.demo.controllers;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
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

    // ===== THANH TOÁN =====
    @PostMapping("/{accountId}")
    public String checkout(@PathVariable Integer accountId) {

        Account account = accountRepo.findById(accountId).orElse(null);
        if (account == null) {
            return "redirect:/cart/" + accountId;
        }

        List<CartDetail> cartList =
                cartDetailRepo.findByAccount_Id(accountId);

        if (cartList.isEmpty()) {
            return "redirect:/cart/" + accountId;
        }

        BigDecimal total = BigDecimal.ZERO;

        // ===== TÍNH TỔNG TIỀN =====
        for (CartDetail item : cartList) {

            BigDecimal price = item.getProduct().getPrice();
            BigDecimal quantity =
                    BigDecimal.valueOf(item.getQuantity());

            total = total.add(price.multiply(quantity));
        }

        // ===== TẠO ORDER =====
        Orders order = new Orders();
        order.setAccount(account);
        order.setCreatedDate(new Date());
        order.setTotal(total);
        order.setFeeship(BigDecimal.ZERO);
        order.setMoneyDiscounted(BigDecimal.ZERO);
        order.setStatus(0);              // 0 = chờ xác nhận
        order.setPaymentStatus(false);   // chưa thanh toán

        ordersRepo.save(order);

        // ===== TẠO ORDER DETAIL =====
        for (CartDetail item : cartList) {

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(item.getProduct());
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getProduct().getPrice()); // 🔥 giờ đã đúng

            orderDetailRepo.save(detail);
        }

        // ===== CẬP NHẬT TỔNG CHI TIÊU ACCOUNT =====
        BigDecimal currentSpending =
                account.getTotalSpending() == null
                        ? BigDecimal.ZERO
                        : account.getTotalSpending();

        account.setTotalSpending(currentSpending.add(total));
        accountRepo.save(account);

        // ===== XOÁ GIỎ HÀNG =====
        cartDetailRepo.deleteAll(cartList);

        return "redirect:/orders/history/" + accountId;
    }
}