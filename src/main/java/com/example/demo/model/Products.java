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
import lombok.ToString;

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

    @DecimalMin(value = "50000", message = "Giá phải lớn hơn 50000")
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal price;

    @Min(value = 0, message = "Số lượng phải lớn hơn hoặc bằng 0")
    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer quantity = 0;

    @Column(columnDefinition = "nvarchar(MAX)")
    private String description;

    @Column
    private Double weight;

    @Temporal(TemporalType.DATE)
    private Date createdDate;

    private boolean available;

    @Column(columnDefinition = "int default 0")
    private Integer discount = 0;

    // =============================
    // RELATIONSHIPS
    // =============================

    @ManyToOne
    @JoinColumn(name = "categoryId")
    @JsonIgnore
    @ToString.Exclude
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    private List<ProductImage> images = new ArrayList<>();

    // =============================
    // HELPER METHODS
    // =============================

    // ✅ Hàm này để Thymeleaf gọi: like.product.mainImage
    public String getMainImage() {
        if (images != null && !images.isEmpty()) {

            // ưu tiên thumbnail trước
            for (ProductImage img : images) {
                if (Boolean.TRUE.equals(img.getThumbnail()) && img.getImage() != null) {
                    return img.getImage();
                }
            }

            // nếu không có thumbnail thì lấy ảnh đầu tiên
            if (images.get(0).getImage() != null) {
                return images.get(0).getImage();
            }
        }

        return "no-image.png";
    }

    // Nếu bạn cần lấy tên category
    public String getCategoryName() {
        return category != null ? category.getName() : "";
    }

    // Nếu cần kiểm tra sản phẩm mới
    public boolean isNewProduct() {
        if (createdDate == null) return false;
        long diff = new Date().getTime() - createdDate.getTime();
        long days = diff / (1000 * 60 * 60 * 24);
        return days <= 7;
    }
}