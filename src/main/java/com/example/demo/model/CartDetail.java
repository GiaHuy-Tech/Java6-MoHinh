package com.example.demo.model;

import jakarta.persistence.*;
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

    @ManyToOne
    @JoinColumn(name = "productId")
    @NotNull(message = "Sản phẩm không được để trống")
    Products product; 

    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    int quantity;

    @Min(value = 0, message = "Đơn giá không hợp lệ")
    int price;
}
