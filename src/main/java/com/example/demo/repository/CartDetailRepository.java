package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Account;
import com.example.demo.model.CartDetail;
import com.example.demo.model.Products;
import com.example.demo.dto.CartStatsDTO;

public interface CartDetailRepository extends JpaRepository<CartDetail, Integer> {

    @Query("SELECT c FROM CartDetail c JOIN FETCH c.product WHERE c.account.id = :accountId")
    List<CartDetail> findCartWithProduct(@Param("accountId") Integer accountId);

    List<CartDetail> findByAccount_Id(Integer accountId);

    Optional<CartDetail> findByAccountAndProduct(Account account, Products product);

    // ===== THỐNG KÊ SẢN PHẨM TRONG GIỎ =====
    @Query("""
        SELECT new com.example.demo.dto.CartStatsDTO(
            p,
            SUM(c.quantity)
        )
        FROM CartDetail c
        JOIN c.product p
        GROUP BY p
        ORDER BY SUM(c.quantity) DESC
    """)
    List<CartStatsDTO> getTopProductsInCarts();
}