package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cart_detail")
public class CartDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
//
    @ManyToOne
    @JoinColumn(name = "cartId")
    @NotNull(message = "Chi tiết giỏ hàng phải thuộc về một giỏ hàng")
    Cart cart;

    // ✅ PHẢI có cart
    @ManyToOne
<<<<<<< Updated upstream
    @JoinColumn(name = "productId")
    @NotNull(message = "Sản phẩm không được để trống")
    Products product;
=======
    @JoinColumn(name = "cart_id")
    private Cart cart;
>>>>>>> Stashed changes

    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    int quantity;

<<<<<<< Updated upstream
    @Min(value = 0, message = "Đơn giá không hợp lệ")
    int price;
}
=======
    // Lưu giá tại thời điểm thêm
    private Double price;

    private int quantity;

    // ===== Getter Setter =====

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public Products getProduct() {
        return product;
    }

    public void setProduct(Products product) {
        this.product = product;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
>>>>>>> Stashed changes
