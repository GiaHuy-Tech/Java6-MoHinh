package com.example.demo.model;

import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Orders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id; // Đã khớp Integer

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    Date createdDate;

    @Column(columnDefinition = "nvarchar(255)")
    String address;

    int total;
    int status;
    int feeship;
    String paymentMethod;
    Boolean paymentStatus;
    String phone;

    // --- THÊM TRƯỜNG NÀY ĐỂ CHẠY SEPAY ---
    @Column(name = "note")
    private String note; // Chứa mã đơn hàng (VD: DH17150022...)
    // --------------------------------------

    @ManyToOne
    @JoinColumn(name = "accountId")
    Account accountId; // Lưu ý tên biến này là accountId

    @OneToMany(mappedBy = "orders")
    List<OrderDetail> orderDetails;
}