package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Comment;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
    List<Comment> findByProduct_IdOrderByCreatedAtDesc(Integer productId);

    // Kiểm tra: User đã đánh giá sản phẩm X trong đơn hàng Y chưa?
    boolean existsByAccount_IdAndProduct_IdAndOrderId(Integer accountId, Integer productId, Integer orderId);

    // Lấy danh sách ID sản phẩm đã đánh giá trong 1 đơn hàng cụ thể
    List<Comment> findByAccount_IdAndOrderId(Integer accountId, Integer orderId);
}