package com.example.demo.model;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;
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
    private Integer id;

    @Column(nullable = false)
    private String code;

    @Column(name = "discount_percent")
    private Integer discountPercent;

    @Column(name = "discount_amount")
    private Double discountAmount;

    @Column(name = "min_order_value")
    private Double minOrderValue;

    @Column(name = "expired_at")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime expiredAt;

    private Boolean active;

<<<<<<< Updated upstream
    // --- THÊM PHẦN NÀY ---
    // Nếu account null -> Ai dùng cũng được (Voucher chung)
    // Nếu account có dữ liệu -> Chỉ người này được dùng
    @ManyToOne
    @JoinColumn(name = "account_id")
    Account account;
=======
    // ✅ THÊM QUAN HỆ VỚI ACCOUNT
    // null = voucher dùng chung
    // có giá trị = voucher riêng của user đó
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;
>>>>>>> Stashed changes
}