package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Account;
import com.example.demo.model.CartDetail;
import com.example.demo.model.Products;

public interface CartDetailRepository extends JpaRepository<CartDetail, Integer> {

    List<CartDetail> findByAccount(Account account);

    Optional<CartDetail> findByAccountAndProduct(Account account, Products product);

}