package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.Products;

public interface ProductRepository extends JpaRepository<Products, Integer> {

	// Lấy 5 sản phẩm mới nhất
	List<Products> findTop5ByOrderByCreatedDateDesc();

	List<Products> findByAvailableTrue();

	List<Products> findByCategoryId(Integer categoryId);

	List<Products> findByPriceBetween(double minPrice, double maxPrice);

	List<Products> findAllByOrderByPriceAsc();

	List<Products> findAllByOrderByPriceDesc();

	List<Products> findAllByOrderByIdDesc();
	
	List<Products> findByNameContainingIgnoreCase(String name);
	
    List<Products> findByCategoryIdAndNameContainingIgnoreCase(Integer categoryId, String name);

}
