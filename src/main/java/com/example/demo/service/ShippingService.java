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
    // Tra zone từ tên tỉnh — dùng để phân loại vùng địa lý
    // ─────────────────────────────────────────────────
    private int getZone(String province) {
        if (province == null) return 3;
        String p = province.trim().toLowerCase();

        if (p.contains("hồ chí minh") || p.contains("hcm") || p.contains("hà nội")) {
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
    // HÀM NỘI BỘ 2: calculateLogic
    // Tính số tiền dựa trên zone + tổng cân nặng
    // ─────────────────────────────────────────────────
    private long calculateLogic(int zone, double totalWeightGram) {
        long baseFee;
        long extraPer500g;

        switch (zone) {
            case 1  -> { baseFee = 15_000; extraPer500g = 5_000; }
            case 2  -> { baseFee = 25_000; extraPer500g = 7_000; }
            default -> { baseFee = 35_000; extraPer500g = 10_000; }
        }

        // Mặc định 500g đầu tiên tính phí baseFee
        // Mỗi 500g tiếp theo tính thêm extraPer500g
        long extraBlocks = (long) Math.max(0, Math.ceil((totalWeightGram - 500) / 500.0));

        return baseFee + (extraBlocks * extraPer500g);
    }

    // ─────────────────────────────────────────────────
    // HÀM NỘI BỘ 3: tính tổng cân nặng từ giỏ hàng
    // ─────────────────────────────────────────────────
    private double getTotalWeight(List<CartDetail> cartItems) {
        if (cartItems == null) return 0;
        return cartItems.stream()
            .mapToDouble(item -> {
                Double w = item.getProduct().getWeight();
                int qty  = item.getQuantity();
                return (w != null ? w : 300.0) * qty; // Mặc định 300g nếu ko có cân nặng
            })
            .sum();
    }

    // ─────────────────────────────────────────────────
    // PUBLIC 1: calculateFee (DÙNG TRONG CHECKOUT CONTROLLER)
    // Tên hàm này đã được đổi để khớp với CheckoutController của bạn
    // ─────────────────────────────────────────────────
    public BigDecimal calculateFee(Address address, List<CartDetail> cartList) {
        // Nếu chưa chọn địa chỉ, trả về phí mặc định 30k
        if (address == null) return BigDecimal.valueOf(30_000);

        int zone = getZone(address.getProvince());
        double totalWeight = getTotalWeight(cartList);
        long fee = calculateLogic(zone, totalWeight);

        return BigDecimal.valueOf(fee);
    }

    // ─────────────────────────────────────────────────
    // PUBLIC 2: calculate (DÙNG CHO AJAX/API TRONG GIỎ HÀNG)
    // Nhận addressId + account, trả về Map JSON
    // ─────────────────────────────────────────────────
    public Map<String, Object> calculate(Long addressId, Account account) {

        Address address = addressRepository.findById(addressId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        List<CartDetail> cartItems = cartDetailRepository
            .findByAccount_Id(account.getId());

        // Gọi lại hàm calculateFee ở trên
        BigDecimal feeShip = calculateFee(address, cartItems);

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