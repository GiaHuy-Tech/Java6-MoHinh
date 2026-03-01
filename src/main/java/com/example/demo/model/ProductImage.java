package com.example.demo.model;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.Data;

@SuppressWarnings("serial")
@Data
@Entity
@Table(name = "products_image")
public class ProductImage implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "varchar(255)")
    private String image;
    
    private Boolean thumbnail; // true nếu là ảnh đại diện

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Products product;
}