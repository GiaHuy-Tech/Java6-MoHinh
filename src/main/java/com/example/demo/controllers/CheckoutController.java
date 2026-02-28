package com.example.demo.controllers;

import java.math.BigDecimal;
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
    private OrderDetailRepository orderDetailRepo;

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

        for (CartDetail item : cartList) {
            BigDecimal price = item.getProduct().getPrice();
            BigDecimal quantity =
                    BigDecimal.valueOf(item.getQuantity());

            total = total.add(price.multiply(quantity));
        }

        // ===== TẠO ORDER =====
        Orders order = new Orders();
        order.setAccount(account);
        order.setTotal(total);
        order.setFeeship(BigDecimal.ZERO);

        ordersRepo.save(order);

        // ===== TẠO ORDER DETAIL =====
        for (CartDetail item : cartList) {

            OrderDetail detail = new OrderDetail();
            detail.setOrders(order);
            detail.setProduct(item.getProduct());
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getProduct().getPrice());

            orderDetailRepo.save(detail);
        }

        // ===== CẬP NHẬT TỔNG CHI TIÊU ACCOUNT =====
        BigDecimal newSpending =
                account.getTotalSpending().add(total);

        account.setTotalSpending(newSpending);
        accountRepo.save(account);

        // ===== XOÁ GIỎ HÀNG =====
        cartDetailRepo.deleteAll(cartList);

        return "redirect:/orders/history/" + accountId;
    }
}