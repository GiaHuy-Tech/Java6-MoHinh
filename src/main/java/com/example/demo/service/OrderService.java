package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.*;
import com.example.demo.repository.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    CartRepository cartRepo;

    @Autowired
    CartDetailRepository cartDetailRepo;

    @Autowired
    OrdersRepository orderRepo;

    @Autowired
    OrdersDetailRepository orderDetailRepo;

    public void createOrder(Account acc, String voucherCode) {

        CartDetail cart = cartRepo.findByAccount(acc);
        if (cart == null) return;

        List<CartDetail> cartList = cartDetailRepo.findByCart(cart);

        double rawTotal = cartList.stream()
                .mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity())
                .sum();

        double discount = 0;
        if (voucherCode != null && voucherCode.equalsIgnoreCase("SALE10")) {
            discount = rawTotal * 0.1;
        }

        double feeShip = rawTotal > 1_000_000 ? 0 : 30000;
        double finalTotal = rawTotal - discount + feeShip;

        Order order = new Order();
        order.setAccount(acc);
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmount(finalTotal);
        order.setStatus("PENDING");

        orderRepo.save(order);

        for (CartDetail cd : cartList) {
            OrderDetail od = new OrderDetail();
            od.setOrder(order);
            od.setProduct(cd.getProduct());
            od.setQuantity(cd.getQuantity());
            od.setPrice(cd.getProduct().getPrice());
            orderDetailRepo.save(od);
        }

        cartDetailRepo.deleteAll(cartList);
    }
}