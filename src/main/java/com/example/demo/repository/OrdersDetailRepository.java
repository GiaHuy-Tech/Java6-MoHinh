package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.OrderDetail;
public interface OrdersDetailRepository extends JpaRepository<OrderDetail, Integer>{

}
