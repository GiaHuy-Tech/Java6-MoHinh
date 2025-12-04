package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.Cart;



import java.util.Optional;

import com.example.demo.model.Account;

public interface CartRepository extends JpaRepository<Cart, Integer> {
    Optional<Cart> findByAccount(Account account);
    Optional<Cart> findById(Integer id);
    Cart findByAccount_Id(Integer accountId);

}