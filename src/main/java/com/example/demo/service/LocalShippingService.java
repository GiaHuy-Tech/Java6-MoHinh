package com.example.demo.service;

import org.springframework.stereotype.Service;

@Service
public class LocalShippingService {

    // ====== TỌA ĐỘ SHOP (ĐỔI THEO SHOP MÀY) ======
    // Ví dụ: Cần Thơ
    private static final double SHOP_LAT = 10.0452;
    private static final double SHOP_LNG = 105.7469;

    // ==============================
    // TÍNH KHOẢNG CÁCH (HAVERSINE)
    // ==============================
    private double distanceKm(double lat1, double lon1,
                              double lat2, double lon2) {

        final int R = 6371; // km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    // ==============================
    // TÍNH PHÍ SHIP (GIỐNG GHN)
    // ==============================
    public int calculateShipping(double customerLat,
                                 double customerLng,
                                 int weightGram,
                                 int orderValue,
                                 boolean isCOD) {

        double km = distanceKm(SHOP_LAT, SHOP_LNG,
                               customerLat, customerLng);

        // ===== PHÍ THEO KHOẢNG CÁCH =====
        int baseFee;

        if (km <= 5) baseFee = 20000;
        else if (km <= 20) baseFee = 30000;
        else if (km <= 50) baseFee = 40000;
        else if (km <= 200) baseFee = 55000;
        else baseFee = 70000;

        // ===== PHÍ THEO CÂN NẶNG =====
        int weightFee = 0;
        if (weightGram > 1000) {
            int extraKg =
                    (int) Math.ceil((weightGram - 1000) / 1000.0);
            weightFee = extraKg * 5000;
        }

        // ===== PHÍ COD =====
        int codFee = 0;
        if (isCOD) {
            codFee = (int) (orderValue * 0.005); // 0.5%
        }

        // ===== PHÍ BẢO HIỂM =====
        int insuranceFee = 0;
        if (orderValue > 1000000) {
            insuranceFee = 5000;
        }

        return baseFee + weightFee + codFee + insuranceFee;
    }
}