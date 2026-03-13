package com.example.demo.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	Page<Products> findByAvailableTrue(Pageable pageable);

	Page<Products> findByCategoryId(Integer categoryId, Pageable pageable);

	Page<Products> findByNameContainingIgnoreCase(String name, Pageable pageable);

	Page<Products> findByCategoryIdAndNameContainingIgnoreCase(Integer categoryId, String name, Pageable pageable);

}
