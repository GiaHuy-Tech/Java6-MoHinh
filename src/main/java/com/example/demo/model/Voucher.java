package com.example.demo.model;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

// Nhập hết các annotation
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    LocalDateTime expiredAt;

    Boolean active;

    // --- THÊM PHẦN NÀY ---
    // Nếu account null -> Ai dùng cũng được (Voucher chung)
    // Nếu account có dữ liệu -> Chỉ người này được dùng
    @ManyToOne
    @JoinColumn(name = "account_id")
    Account account;
}