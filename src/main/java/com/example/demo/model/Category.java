package com.example.demo.model;

import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Tên không được bỏ trống")
    @Column(unique = true, columnDefinition = "nvarchar(255)")
    private String name;

    @NotBlank(message = "Hình không được bỏ trống")
    @Column(columnDefinition = "nvarchar(255)")
    private String image;

    // ❌ QUAN TRỌNG: loại khỏi toString để tránh vòng lặp
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Products> products;
}
