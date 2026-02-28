package com.example.demo.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.GhnFeeRequest;
import com.example.demo.service.GhnShippingService;

@RestController
@RequestMapping("/api/shipping")
public class ShippingApiController {

    @Autowired
    private GhnShippingService ghnService;

    @PostMapping("/fee")
    public Integer tinhTienShip(@RequestBody GhnFeeRequest request) {
        return ghnService.tinhPhi(request);
    }
}


