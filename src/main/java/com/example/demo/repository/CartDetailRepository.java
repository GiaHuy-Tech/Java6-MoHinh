package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Account;
import com.example.demo.model.CartDetail;
import com.example.demo.model.Products; // Đảm bảo tên class Entity sản phẩm của bạn đúng là Products (có 's') hay Product

@Repository
public interface CartDetailRepository extends JpaRepository<CartDetail, Integer> {
    
    // 1. Tìm danh sách giỏ hàng theo ID người dùng
    // (Vì CartDetail nối trực tiếp với Account, nên dùng Account_Id)
    List<CartDetail> findByAccountId(Integer accountId);

    // 2. Tìm chi tiết giỏ hàng theo Account và Product (để check xem sản phẩm đã có trong giỏ chưa)
    // Thay vì findByCartAndProduct, ta dùng findByAccountAndProduct
    Optional<CartDetail> findByAccountAndProduct(Account account, Products product);
}