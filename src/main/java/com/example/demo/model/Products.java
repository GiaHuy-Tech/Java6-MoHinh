package com.example.demo.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
@Table(name = "products")
public class Products {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Column(columnDefinition = "nvarchar(255)")
    private String name;

    @Column(columnDefinition = "nvarchar(255)")
    private String image;

    @Min(value = 50000, message = "Giá phải lớn hơn 50000")
    private int price;

    // ✅ MỚI THÊM: CỘT SỐ LƯỢNG
    @Min(value = 0, message = "Số lượng phải lớn hơn hoặc bằng 0")
    // Thêm columnDefinition = "int default 0" để SQL Server tự điền số 0 cho dữ liệu cũ
    @Column(nullable = false, columnDefinition = "int default 0") 
    private Integer quantity = 0;

    @Temporal(TemporalType.DATE)
    private Date createdDate;

    private boolean available;

    // 🔥 Chặn vòng lặp JSON
    @ManyToOne
    @JoinColumn(name = "categoryId")
    @JsonIgnore
    private Category category;

    @Column(columnDefinition = "nvarchar(MAX)")
    private String description;
    
    @Column
    private Double weight; // kg

    // 🔥 Chặn vòng lặp JSON
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ProductImage> images = new ArrayList<>();

    public void addImage(ProductImage img) {
        images.add(img);
        img.setProduct(this);
    }

    public void removeImage(ProductImage img) {
        images.remove(img);
        img.setProduct(null);
    }
}