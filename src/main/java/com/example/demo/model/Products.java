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

    // 🔥 GIÁ SẢN PHẨM
    @DecimalMin(value = "50000", message = "Giá phải lớn hơn 50000")
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal price;

    // 🔥 SỐ LƯỢNG
    @Min(value = 0, message = "Số lượng phải lớn hơn hoặc bằng 0")
    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer quantity = 0;

    // 🔥 MÔ TẢ & TRỌNG LƯỢNG
    @Column(columnDefinition = "nvarchar(MAX)")
    private String description;

    @Column
    private Double weight; // kg

    // 🔥 NGÀY TẠO & TRẠNG THÁI
    @Temporal(TemporalType.DATE)
    private Date createdDate;

    private boolean available;
    
    // Thêm trường Discount (Giảm giá) nếu HTML của bạn có dùng ${p.discount}
    // Nếu trong Database chưa có cột này, bạn có thể thêm @Transient để test, 
    // hoặc thêm cột thật vào DB. Ở đây tôi để mặc định là 0.
    @Column(columnDefinition = "int default 0")
    private Integer discount = 0;

    // ==========================================
    // CÁC MỐI QUAN HỆ (RELATIONSHIPS)
    // ==========================================

    @ManyToOne
    @JoinColumn(name = "categoryId")
    @JsonIgnore
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ProductImage> images = new ArrayList<>();

    // ==========================================
    // CÁC HÀM XỬ LÝ LOGIC (HELPER METHODS)
    // ==========================================

    public void addImage(ProductImage img) {
        images.add(img);
        img.setProduct(this);
    }

    public void removeImage(ProductImage img) {
        images.remove(img);
        img.setProduct(null);
    }

    // 1. Logic xác định sản phẩm mới (Dùng cho nhãn NEW)
    // Thymeleaf gọi bằng: ${p.newProduct}
    public boolean isNewProduct() {
        if (createdDate == null) {
            return false;
        }
        long currentTime = new Date().getTime();
        long createdTime = createdDate.getTime();
        long diff = currentTime - createdTime;
        long days = diff / (24 * 60 * 60 * 1000);
        
        return days <= 7; // Mới trong vòng 7 ngày
    }

    // 2. Lấy ảnh đại diện
    // Thymeleaf gọi bằng: ${p.mainImage}
    public String getMainImage() {
        if (images == null || images.isEmpty()) {
            return "no-image.jpg"; 
        }
        // Ưu tiên ảnh thumbnail
        for (ProductImage img : images) {
            if (Boolean.TRUE.equals(img.getThumbnail())) {
                return img.getImage();
            }
        }
        // Nếu không có, lấy ảnh đầu tiên
        return images.get(0).getImage();
    }

    // 3. Lấy tên danh mục an toàn (tránh lỗi null)
    // Thymeleaf gọi bằng: ${p.categoryName}
    public String getCategoryName() {
        return category != null ? category.getName() : "Khác";
    }
}