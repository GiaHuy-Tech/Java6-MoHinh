package com.example.demo.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;

@SuppressWarnings("serial")
@Data
@Entity
@Table(name = "orders")
public class Orders implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date")
    private Date createdDate;

    @Column(precision = 15, scale = 2)
    private BigDecimal feeship = BigDecimal.ZERO;

    @Column(columnDefinition = "nvarchar(500)")
    private String note;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_status")
    private Boolean paymentStatus = false;

    private String phone;

    // 0: Chờ, 1: Xác nhận, 2: Giao, 3: Hoàn tất, 4: Hủy
    private Integer status = 0;

    @Column(name = "voucher_code")
    private String voucherCode;

    @Column(name = "money_discounted", precision = 15, scale = 2)
    private BigDecimal moneyDiscounted = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderDetail> orderDetails;
}