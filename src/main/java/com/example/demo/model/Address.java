package com.example.demo.model;

import org.hibernate.annotations.Nationalized; // THÊM DÒNG NÀY
import jakarta.persistence.*;
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

    // THÊM @Nationalized VÀO CÁC TRƯỜNG DƯỚI ĐÂY
    @Nationalized
    @Column(name = "recipient_name", columnDefinition = "nvarchar(255)")
    private String recipientName;

    @Column(name = "recipient_phone")
    private String recipientPhone;

    @Nationalized
    @Column(columnDefinition = "nvarchar(255)")
    private String province;

    @Nationalized
    @Column(columnDefinition = "nvarchar(255)")
    private String district;

    @Nationalized
    @Column(columnDefinition = "nvarchar(255)")
    private String ward; 

    @Column(name = "ward_code")
    private String wardCode; 

    @Nationalized
    @Column(columnDefinition = "nvarchar(MAX)")
    private String detail;

    private Double latitude;
    private Double longitude;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @PrePersist
    @PreUpdate
    public void normalize() {
        if (recipientName != null) recipientName = recipientName.trim();
        if (recipientPhone != null) recipientPhone = recipientPhone.trim();
        if (province != null) province = province.trim();
        if (district != null) district = district.trim();
        if (ward != null) ward = ward.trim();
        if (detail != null) detail = detail.trim();
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (detail != null && !detail.isBlank()) sb.append(detail.trim());
        if (ward != null && !ward.isBlank()) sb.append((sb.length() > 0 ? ", " : "") + ward.trim());
        if (district != null && !district.isBlank()) sb.append((sb.length() > 0 ? ", " : "") + district.trim());
        if (province != null && !province.isBlank()) sb.append((sb.length() > 0 ? ", " : "") + province.trim());
        return sb.length() > 0 ? sb.toString() : "Chưa có địa chỉ";
    }
}