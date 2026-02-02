package com.example.demo.model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vouchers_detail")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne
    @JoinColumn(name = "vouchers_id")
    private Voucher voucher;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Orders order;

    @Column(name = "is_used")
    private Boolean isUsed;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "used_at")
    private Date usedAt;

    private String status;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "received_at")
    private Date receivedAt;
}