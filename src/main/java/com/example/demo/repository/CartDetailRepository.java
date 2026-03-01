package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.CartDetail;

public interface CartDetailRepository extends JpaRepository<CartDetail, Integer> {
    
    // Tìm danh sách món hàng theo ID của tài khoản
    List<CartDetail> findByAccount_Id(Integer accountId);
}