package com.example.demo.model;

import java.time.LocalDateTime;
import jakarta.persistence.*; // Nhập hết các annotation
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
@Table(name = "vouchers")
public class Voucher {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(nullable = false, unique = false)
    String code;

    @Column(name = "discount_percent")
    Integer discountPercent;

    @Column(name = "discount_amount")
    Double discountAmount; 

    @Column(name = "min_order_value")
    Double minOrderValue;

    @Column(name = "expired_at")
    LocalDateTime expiredAt;

    Boolean active;

    // --- THÊM PHẦN NÀY ---
    // Nếu account null -> Ai dùng cũng được (Voucher chung)
    // Nếu account có dữ liệu -> Chỉ người này được dùng
    @ManyToOne
    @JoinColumn(name = "account_id")
    Account account; 
}