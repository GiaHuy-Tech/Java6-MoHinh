package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Liên kết đến sản phẩm
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Products product; 

    private LocalDateTime createdDate;
    
    // Liên kết đến người dùng
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account; 

    @Column(columnDefinition = "nvarchar(255)", nullable = false)
    @Size(max = 255)
    private String content; // Nội dung bình luận

    @Column(name = "imagage", columnDefinition = "nvarchar(255)")
    private String image; 

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}