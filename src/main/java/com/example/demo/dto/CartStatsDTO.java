package com.example.demo.dto;

import com.example.demo.model.Products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartStatsDTO {
    private Products product;
    private Long totalQuantity; // Tổng số lượng sản phẩm này trong tất cả giỏ hàng
}