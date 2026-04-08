package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShippingService {

    private final AddressRepository addressRepository;
    private final CartDetailRepository cartDetailRepository;

    // ─────────────────────────────────────────────────
    // HÀM NỘI BỘ 1: getZone
    // Tra zone từ tên tỉnh — hardcode, không cần DB
    // ─────────────────────────────────────────────────
    private int getZone(String province) {
        if (province == null) return 3;
        String p = province.trim().toLowerCase();

        if (p.contains("hồ chí minh") || p.contains("hcm")
         || p.contains("hà nội")) {
            return 1;
        }
        if (p.contains("bình dương") || p.contains("đồng nai")
         || p.contains("cần thơ")   || p.contains("đà nẵng")
         || p.contains("hải phòng") || p.contains("long an")
         || p.contains("bà rịa")    || p.contains("vũng tàu")
         || p.contains("khánh hòa") || p.contains("nha trang")) {
            return 2;
        }
        return 3;
    }

    // ─────────────────────────────────────────────────
    // HÀM NỘI BỘ 2: calculateFee
    // Tính phí theo zone + tổng cân nặng
    // Zone 1: 15k base + 5k/500g vượt
    // Zone 2: 25k base + 7k/500g vượt
    // Zone 3: 35k base + 10k/500g vượt
    // ─────────────────────────────────────────────────
    private long calculateFee(int zone, double totalWeightGram) {
        long baseFee;
        long extraPer500g;

        switch (zone) {
            case 1  -> { baseFee = 15_000; extraPer500g = 5_000; }
            case 2  -> { baseFee = 25_000; extraPer500g = 7_000; }
            default -> { baseFee = 35_000; extraPer500g = 10_000; }
        }

        // Số block 500g vượt quá 500g đầu
        // VD: 1300g → vượt 800g → ceil(800/500)=2 block → +2×extra
        long extraBlocks = (long) Math.max(0,
            Math.ceil((totalWeightGram - 500) / 500.0));

        return baseFee + (extraBlocks * extraPer500g);
    }

    // ─────────────────────────────────────────────────
    // HÀM NỘI BỘ 3: tính tổng cân từ danh sách CartDetail
    // ─────────────────────────────────────────────────
    private double getTotalWeight(List<CartDetail> cartItems) {
        return cartItems.stream()
            .mapToDouble(item -> {
                Double w = item.getProduct().getWeight();
                int qty  = item.getQuantity();
                return (w != null ? w : 300.0) * qty;
            })
            .sum();
    }

    // ─────────────────────────────────────────────────
    // PUBLIC 1: calculateShipping
    // CheckoutController (GET + POST) gọi hàm này
    // Nhận thẳng cartList + address, trả về BigDecimal
    // ─────────────────────────────────────────────────
    public BigDecimal calculateShipping(List<CartDetail> cartList, Address address) {
        if (address == null) return BigDecimal.valueOf(30_000); // fallback

        int zone = getZone(address.getProvince());
        double totalWeight = getTotalWeight(cartList);
        long fee = calculateFee(zone, totalWeight);

        return BigDecimal.valueOf(fee);
    }

    // ─────────────────────────────────────────────────
    // PUBLIC 2: calculate
    // ShippingController (API endpoint) gọi hàm này
    // Nhận addressId + account, trả về Map JSON
    // ─────────────────────────────────────────────────
    public Map<String, Object> calculate(Long addressId, Account account) {

        Address address = addressRepository.findById(addressId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        List<CartDetail> cartItems = cartDetailRepository
            .findByAccount_Id(account.getId());

        BigDecimal feeShip = calculateShipping(cartItems, address);

        BigDecimal rawTotal = cartItems.stream()
            .map(i -> i.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(i.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalTotal = rawTotal.add(feeShip);

        return Map.of(
            "feeShip",    feeShip.longValue(),
            "finalTotal", finalTotal
        );
    }
}