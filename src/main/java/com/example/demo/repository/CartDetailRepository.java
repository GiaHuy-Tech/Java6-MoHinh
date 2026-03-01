package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.demo.dto.CartStatsDTO;
import com.example.demo.model.Account;
import com.example.demo.model.CartDetail;
import com.example.demo.model.Products;

public interface CartDetailRepository 
        extends JpaRepository<CartDetail, Integer> {

    // Lấy toàn bộ giỏ hàng theo accountId
    List<CartDetail> findByAccount_Id(Integer accountId);

    // 🔥 THÊM DÒNG NÀY (QUAN TRỌNG)
    Optional<CartDetail> findByAccountAndProduct(Account account, Products product);
    
 // Câu lệnh JPQL: Gom nhóm theo Product và tính tổng quantity, sắp xếp giảm dần
    @Query("SELECT new com.example.demo.dto.CartStatsDTO(c.product, SUM(c.quantity)) " +
           "FROM CartDetail c " +
           "GROUP BY c.product " +
           "ORDER BY SUM(c.quantity) DESC")
    List<CartStatsDTO> getTopProductsInCarts();
}