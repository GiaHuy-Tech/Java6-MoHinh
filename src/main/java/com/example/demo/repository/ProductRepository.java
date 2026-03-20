package com.example.demo.repository;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.Products;

public interface ProductRepository extends JpaRepository<Products, Integer> {

    // Lấy 5 sản phẩm mới nhất (dùng cho banner/slideshow)
    List<Products> findTop5ByOrderByCreatedDateDesc();

    // HÀM MỚI THÊM: Tìm kiếm top 5 sản phẩm theo tên (không phân biệt hoa thường)
    // Phục vụ tính năng Live Search
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
    
    // 1. Chỉ lọc theo khoảng giá
    Page<Products> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    // 2. Lọc theo danh mục + khoảng giá
    Page<Products> findByCategoryIdAndPriceBetween(Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    // 3. Lọc theo từ khóa + khoảng giá
    Page<Products> findByNameContainingIgnoreCaseAndPriceBetween(String name, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    // 4. Lọc tổng hợp: Danh mục + Từ khóa + Khoảng giá
    Page<Products> findByCategoryIdAndNameContainingIgnoreCaseAndPriceBetween(Integer categoryId, String name, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

}