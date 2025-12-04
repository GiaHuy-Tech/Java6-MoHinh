package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Cart;
import com.example.demo.model.CartDetail;
import com.example.demo.model.Products;

@Repository
public interface CartDetailRepository extends JpaRepository<CartDetail, Integer> {
    List<CartDetail> findByCart_Account_Id(Integer accountId);
    Optional<CartDetail> findByCartAndProduct(Cart cart, Products product);
	
}
