package com.example.demo.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.model.Address;
import com.example.demo.model.CartDetail;

@Service
public class ShippingService {

    public BigDecimal calculateFee(Address address, List<CartDetail> cartList) {

        try {
            if (address == null) return BigDecimal.valueOf(30000);

            RestTemplate restTemplate = new RestTemplate();
            String url = "http://localhost:8080/mock/ghn/fee";

            // ===== WEIGHT =====
            int weight = cartList.stream()
                    .mapToInt(i -> i.getQuantity() * 500)
                    .sum();

            // ===== FIX PROVINCE (QUAN TRỌNG) =====
            String province = address.getProvince();
            if (province != null) {
                province = province
                        .replace("Thành phố ", "")
                        .replace("Tỉnh ", "")
                        .trim();
            }

            // ===== BODY =====
            Map<String, Object> body = new HashMap<>();
            body.put("province", province);
            body.put("district", address.getDistrict());
            body.put("weight", weight);

            System.out.println("=== CALL GHN ===");
            System.out.println("Province gửi đi: " + province);

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(url, body, Map.class);

            Map data = (Map) response.getBody().get("data");
            Integer fee = (Integer) data.get("total");

            return BigDecimal.valueOf(fee);

        } catch (Exception e) {
            e.printStackTrace();
            return BigDecimal.valueOf(30000);
        }
    }
}