package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Account;
import com.example.demo.model.Like;
import com.example.demo.model.Products;

public interface LikeRepository extends JpaRepository<Like, Integer> {

    // --- GIỮ LẠI CHO STATSCONTROLLER (HẾT LỖI 500) ---
    @Query("SELECT w.product FROM Like w GROUP BY w.product ORDER BY COUNT(w.product) DESC LIMIT 1")
    Optional<Products> findTopByMostLiked();

    // --- GIỮ LẠI CÁC HÀM CŨ CỦA BẠN ---
    List<Like> findByAccount(Account account);

    default List<Like> findAllOrderByNewest() {
        return findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    // --- THÊM MỚI CHO TRANG WISHLIST (ĐỂ ĐỔ DỮ LIỆU ĐÚNG USER) ---
    @Query("SELECT l FROM Like l WHERE l.account.id = :accountId ORDER BY l.likedAt DESC")
    List<Like> findByAccountId(@Param("accountId") Integer accountId);

    @Query("SELECT l FROM Like l WHERE l.account.id = :accountId AND l.product.id = :productId")
    Like findByAccountIdAndProductId(@Param("accountId") Integer accountId, @Param("productId") Integer productId);
}