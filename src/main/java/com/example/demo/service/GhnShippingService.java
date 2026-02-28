package com.example.demo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.GhnFeeRequest;
import com.example.demo.model.Account;
import com.example.demo.model.Address;
import com.example.demo.model.CartDetail;
import com.example.demo.repository.AddressRepository;

@Service
public class GhnShippingService {

    private static final int BASE_FEE = 30000;   // phí cơ bản
    private static final int EXTRA_PER_KG = 5000;

    public int tinhPhiTuCart(Account account, List<CartDetail> cartDetails) {

        // ❌ Không có sản phẩm → 30k
        if (cartDetails == null || cartDetails.isEmpty()) {
            return BASE_FEE;
        }

        int totalWeightGram = 0;

        for (CartDetail cd : cartDetails) {
            Double kg = cd.getProduct().getWeight();

            // ❌ KHÔNG RÕ CÂN NẶNG → 30K
            if (kg == null || kg <= 0) {
                return BASE_FEE;
            }

            int gram = (int) Math.ceil(kg * 1000);
            totalWeightGram += gram * cd.getQuantity();
        }

        return tinhPhiTheoCanNang(totalWeightGram);
    }

    // ================= TÍNH THEO CÂN NẶNG =================
    private int tinhPhiTheoCanNang(int totalWeightGram) {

        if (totalWeightGram <= 0) {
            return BASE_FEE;
        }

        int totalKg = (int) Math.ceil(totalWeightGram / 1000.0);

        return BASE_FEE + totalKg * EXTRA_PER_KG;
    }
}
