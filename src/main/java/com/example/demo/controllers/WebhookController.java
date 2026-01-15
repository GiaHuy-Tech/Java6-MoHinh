package com.example.demo.controllers;

import com.example.demo.dto.SePayWebhookDto;
import com.example.demo.model.Orders;
import com.example.demo.repository.OrdersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sepay")
public class WebhookController {

    @Autowired
    private OrdersRepository orderRepo;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleSePayWebhook(@RequestBody SePayWebhookDto webhookData) {
        String orderCode = webhookData.getContent(); // Lấy mã DH...
        
        // Tìm đơn hàng theo mã DH... (trường note)
        Orders order = orderRepo.findByNote(orderCode).orElse(null);

        if (order == null) return ResponseEntity.ok("Order not found");

        if (Boolean.TRUE.equals(order.getPaymentStatus())) return ResponseEntity.ok("Already paid");

        // So sánh số tiền (webhookData gửi Long, Order lưu int -> cẩn thận so sánh)
        if (webhookData.getTransferAmount() >= order.getTotal()) {
            order.setPaymentStatus(true);
            order.setStatus(1); // Đã xác nhận
            orderRepo.save(order);
        }

        return ResponseEntity.ok("Success");
    }
}