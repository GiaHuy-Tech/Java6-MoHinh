package com.example.demo.model;

import java.io.Serializable;
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

    @Column(name = "created_date")
    private Date createdDate;

    private Double feeship;
    
    @Column(columnDefinition = "nvarchar(500)")
    private String note;
    
    @Column(name = "payment_method")
    private String paymentMethod;
    
    @Column(name = "payment_status")
    private Boolean paymentStatus; // true: đã thanh toán
    
    private String phone;
    private Integer status; // 0: Chờ, 1: Xác nhận, 2: Giao, 3: Hoàn tất, 4: Hủy
    
    @Column(name = "voucher_code")
    private String voucherCode;
    
    @Column(name = "money_discounted")
    private Double moneyDiscounted;
    
    private Double total;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;
    
    // Liên kết với bảng Address để biết giao đến đâu
    @ManyToOne
    @JoinColumn(name = "address_id") 
    private Address address;

    @OneToMany(mappedBy = "order")
    private List<OrderDetail> orderDetails;
}