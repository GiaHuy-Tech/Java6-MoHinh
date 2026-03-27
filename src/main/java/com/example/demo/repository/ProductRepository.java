package com.example.demo.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Products;

public interface ProductRepository extends JpaRepository<Products, Integer> {

    // 🔥 FIX CHÍNH: load luôn images
    @Query("SELECT p FROM Products p LEFT JOIN FETCH p.images WHERE p.id = :id")
    Products findByIdWithImages(@Param("id") Integer id);

    List<Products> findTop5ByOrderByCreatedDateDesc();
    List<Products> findTop5ByNameContainingIgnoreCase(String name);
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

    Page<Products> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    Page<Products> findByCategoryIdAndPriceBetween(Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    Page<Products> findByNameContainingIgnoreCaseAndPriceBetween(String name, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    Page<Products> findByCategoryIdAndNameContainingIgnoreCaseAndPriceBetween(Integer categoryId, String name, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}