package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Products product; 

    // Lưu ý: Bạn có 2 trường thời gian trong model cũ, mình sẽ dùng createdAt làm chính
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account; 

    @Column(columnDefinition = "nvarchar(255)", nullable = false)
    @Size(max = 255)
    private String content;

    // Giữ nguyên tên cột là "imagage" theo DB của bạn
    @Column(name = "imagage", columnDefinition = "nvarchar(255)")
    private String image; 

    // --- THÊM TRƯỜNG ĐÁNH GIÁ SAO ---
    @Column(name = "rating")
    @Min(1) @Max(5)
    private Integer rating; // 1 đến 5 sao
}