package com.example.demo.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Products product;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    // Trường mới: Lưu ID đơn hàng để kiểm soát đánh giá theo từng lần mua
    private Integer orderId; 

    @Column(columnDefinition = "nvarchar(255)")
    private String content;

    private Integer rating; 

    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "imagage") 
    private String image;
}