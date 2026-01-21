package com.example.demo.model;


import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
	Integer id;
	@NotBlank (message = "tên sản phẩm không được để trống")
	@Column(columnDefinition = "nvarchar(255)")
	String name;
	@Column(columnDefinition = "nvarchar(255)")
	String image;
	@Min (value = 50000, message = "Giá phải lớn hơn 50000")
	int price;
	@Temporal(TemporalType.DATE)
	Date createdDate;
	boolean available;
	@ManyToOne @JoinColumn(name = "categoryId")
	Category category;
	@Column(columnDefinition = "nvarchar(MAX)") 
    private String description;
}
