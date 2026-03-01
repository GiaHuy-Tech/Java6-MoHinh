package com.example.demo.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
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

    // 🔥 SỬA CHUẨN TIỀN TỆ
    @DecimalMin(value = "50000", message = "Giá phải lớn hơn 50000")
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal price;

    // ✅ SỐ LƯỢNG
    @Min(value = 0, message = "Số lượng phải lớn hơn hoặc bằng 0")
    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer quantity = 0;

    @Temporal(TemporalType.DATE)
    private Date createdDate;

    private boolean available;

    // 🔥 Quan hệ Category
    @ManyToOne
    @JoinColumn(name = "categoryId")
    @JsonIgnore
    private Category category;

    @Column(columnDefinition = "nvarchar(MAX)")
    private String description;

    @Column
    private Double weight; // kg

    // 🔥 Danh sách ảnh
    @OneToMany(mappedBy = "product",
               cascade = CascadeType.ALL,
               orphanRemoval = true)
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
// --- THÊM ĐOẠN NÀY VÀO CUỐI FILE PRODUCTS.JAVA ---
    
    // Hàm này giúp Thymeleaf gọi được bằng cú pháp: ${product.mainImage}
    public String getMainImage() {
        if (images == null || images.isEmpty()) {
            return "no-image.jpg"; // Tên ảnh mặc định nếu sản phẩm chưa có ảnh
        }
        
        // 1. Ưu tiên tìm ảnh được đánh dấu là Thumbnail (true)
        for (ProductImage img : images) {
            if (Boolean.TRUE.equals(img.getThumbnail())) {
                return img.getImage();
            }
        }
        
        // 2. Nếu không có thumbnail nào, lấy ảnh đầu tiên trong danh sách
        return images.get(0).getImage();
    }
}