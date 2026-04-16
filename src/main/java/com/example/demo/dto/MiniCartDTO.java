package com.example.demo.dto;

import java.math.BigDecimal;

import com.example.demo.model.CartDetail;

public class MiniCartDTO {

    public Integer id;
    public Integer quantity;
    public String name;
    public String image;
    public BigDecimal price;

    public MiniCartDTO(CartDetail c) {
        this.id = c.getId();
        this.quantity = c.getQuantity();
        this.name = c.getProduct().getName();
        this.image = c.getProduct().getMainImage();
        this.price = c.getProduct().getPrice();
    }
}