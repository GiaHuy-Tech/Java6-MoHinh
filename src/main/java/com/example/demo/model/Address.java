package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "address")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "recipient_name",columnDefinition = "nvarchar(255)")
    private String recipientName;

    @Column(name = "recipient_phone")
    private String recipientPhone;

    @Column(columnDefinition = "nvarchar(255)")
    private String province;
    
    @Column(columnDefinition = "nvarchar(255)")
    private String district;
    
    @Column(columnDefinition = "nvarchar(MAX)") // detail thường dài nên để MAX hoặc 500
    private String detail;

    @Column(name = "ward_code")
    private String wardCode;

    @Column(name = "is_default")
    private Boolean isDefault;

    // --- Đã sửa: Nối chuỗi để tạo địa chỉ hoàn chỉnh ---
    public String getFullAddress() {
        String fullAddress = "";
        
        if (detail != null && !detail.trim().isEmpty()) {
            fullAddress += detail.trim();
        }
        if (district != null && !district.trim().isEmpty()) {
            fullAddress += (fullAddress.isEmpty() ? "" : ", ") + district.trim();
        }
        if (province != null && !province.trim().isEmpty()) {
            fullAddress += (fullAddress.isEmpty() ? "" : ", ") + province.trim();
        }
        
        // Nếu tất cả đều trống thì trả về câu thông báo mặc định
        return fullAddress.isEmpty() ? "Chưa có thông tin chi tiết" : fullAddress;
    }
}