package com.example.demo.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

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
