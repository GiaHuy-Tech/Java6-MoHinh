package com.example.demo.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    private String image; // ảnh đại diện

    @Min(value = 50000, message = "Giá phải lớn hơn 50000")
    private int price;

    @Temporal(TemporalType.DATE)
    private Date createdDate;

    private boolean available;

    @ManyToOne
    @JoinColumn(name = "categoryId")
    private Category category;

    @Column(columnDefinition = "nvarchar(MAX)")
    private String description;

    // ✅ ẢNH PHỤ
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    // ✅ BẮT BUỘC THÊM HÀM NÀY
    public void addImage(ProductImage img) {
        images.add(img);
        img.setProduct(this);
    }

    public void removeImage(ProductImage img) {
        images.remove(img);
        img.setProduct(null);
    }
}
