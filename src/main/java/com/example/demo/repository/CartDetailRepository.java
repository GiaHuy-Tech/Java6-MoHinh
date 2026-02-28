package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.CartDetail;

public interface CartDetailRepository extends JpaRepository<CartDetail, Integer> {

    // Lấy giỏ hàng theo accountId
    List<CartDetail> findByAccount_Id(Integer accountId);

}