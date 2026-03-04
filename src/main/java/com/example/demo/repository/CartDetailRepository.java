package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Account;
import com.example.demo.model.CartDetail;
import com.example.demo.model.Products;

public interface CartDetailRepository extends JpaRepository<CartDetail, Integer> {

    // 🔥 Load luôn product tránh N+1
    @Query("SELECT c FROM CartDetail c JOIN FETCH c.product WHERE c.account.id = :accountId")
    List<CartDetail> findCartWithProduct(@Param("accountId") Integer accountId);

    Optional<CartDetail> findByAccountAndProduct(Account account, Products product);
}