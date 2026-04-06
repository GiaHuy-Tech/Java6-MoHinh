package com.example.demo.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mock/ghn")
public class MockGHNController {

    private Map<String, double[]> provinceMap = new HashMap<>() {{
        put("Cần Thơ", new double[]{10.0452, 105.7469});
        put("Hồ Chí Minh", new double[]{10.8231, 106.6297});
        put("Hà Nội", new double[]{21.0285, 105.8542});
        put("Thanh Hóa", new double[]{19.8067, 105.7852});
        put("Cà Mau", new double[]{9.1769, 105.1524});
    }};

    @PostMapping("/fee")
    public Map<String, Object> calculateFee(@RequestBody Map<String, Object> body) {

        String province = (String) body.get("province");
        int weight = (int) body.getOrDefault("weight", 500);

        // ===== FIX STRING =====
        if (province != null) {
            province = province
                    .replace("Thành phố ", "")
                    .replace("Tỉnh ", "")
                    .trim();
        }

        System.out.println("=== MOCK GHN ===");
        System.out.println("Province nhận: " + province);

        if (province == null || !provinceMap.containsKey(province)) {
            System.out.println("❌ Không tìm thấy tỉnh → fallback 30k");
            return response(30000);
        }

        // ===== KHO = CẦN THƠ =====
        double[] origin = provinceMap.get("Cần Thơ");
        double[] dest = provinceMap.get(province);

        double distance = calculateDistance(origin, dest);

        System.out.println("Distance = " + distance + " km");

        int fee;

        if (distance < 50) fee = 20000;
        else if (distance < 200) fee = 30000;
        else if (distance < 800) fee = 50000;
        else fee = 70000;

        // ===== WEIGHT =====
        fee += (weight / 1000) * 5000;

        return response(fee);
    }

    private Map<String, Object> response(int fee) {
        Map<String, Object> data = new HashMap<>();
        data.put("total", fee);

        Map<String, Object> res = new HashMap<>();
        res.put("data", data);

        return res;
    }

    private double calculateDistance(double[] a, double[] b) {
        double R = 6371;

        double dLat = Math.toRadians(b[0] - a[0]);
        double dLon = Math.toRadians(b[1] - a[1]);

        double lat1 = Math.toRadians(a[0]);
        double lat2 = Math.toRadians(b[0]);

        double x = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.sin(dLon / 2) * Math.sin(dLon / 2)
                * Math.cos(lat1) * Math.cos(lat2);

        double c = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));

        return R * c;
    }
}