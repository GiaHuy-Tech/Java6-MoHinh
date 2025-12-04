package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Products;
import com.example.demo.repository.ProductRepository;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    // Đếm tổng số sản phẩm
    public long countProducts() {
        return productRepository.count();
    }

    // Tìm sản phẩm theo ID
    public Products findById(Integer id) {
        return productRepository.findById(id).orElse(null);
    }

    // Lấy danh sách tất cả sản phẩm
    public List<Products> findAll() {
        return productRepository.findAll();
    }

    // Lưu hoặc cập nhật sản phẩm
    public Products save(Products product) {
        return productRepository.save(product);
    }

    // Xóa sản phẩm theo ID
    public boolean deleteById(Integer id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Tìm sản phẩm theo danh mục
    public List<Products> findByCategoryId(Integer categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    // Lấy danh sách 5 sản phẩm mới nhất
    public List<Products> findTop5Newest() {
        return productRepository.findTop5ByOrderByCreatedDateDesc();
    }
}
