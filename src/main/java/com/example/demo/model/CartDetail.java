package com.example.demo.model;

import java.io.Serializable;
import java.util.Date;
import java.math.BigDecimal; // Đảm bảo có dòng này
import jakarta.persistence.*;
import lombok.Data;

@SuppressWarnings("serial")
@Data
@Entity
@Table(name = "cart_detail")
public class CartDetail implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Sửa chỗ này: Bảo Hibernate đừng bao giờ động vào cột này khi Insert/Update
    @Column(name = "cart_id", insertable = false, updatable = false)
    private Integer cartId; 

    private Integer quantity;
    private BigDecimal price; 

    @Column(name = "createdate")
    @Temporal(TemporalType.DATE)
    private Date createDate = new Date();

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Products product;
}