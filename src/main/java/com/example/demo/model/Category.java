package com.example.demo.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "categories")
public class Category {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Integer id;
	@NotBlank(message = "tên không đưuọc bỏ trống")
	@Column(unique = true, columnDefinition = "nvarchar(255)")
	String name;
    @NotBlank(message = "Hình không được bỏ trống")
	@Column(columnDefinition = "nvarchar(255)")
	String image;
	@OneToMany(mappedBy = "category")
	List<Products> products;
}