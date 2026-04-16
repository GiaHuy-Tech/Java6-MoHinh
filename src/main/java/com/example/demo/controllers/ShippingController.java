package com.example.demo.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Account;
import com.example.demo.service.ShippingService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    // Endpoint này đã được JS trong checkout.html gọi sẵn:
    // fetch(`/api/shipping/calculate?addressId=` + addressId)
    @GetMapping("/api/shipping/calculate")
    public ResponseEntity<?> calculate(
            @RequestParam Long addressId,
            HttpSession session) {

        // ⚠️ Kiểm tra lại key session của bạn
        // Nếu bạn lưu là session.setAttribute("user", acc) thì đổi "account" → "user"
        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return ResponseEntity.status(401).body("Chưa đăng nhập");
        }

        try {
            Map<String, Object> result = shippingService.calculate(addressId, account);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}