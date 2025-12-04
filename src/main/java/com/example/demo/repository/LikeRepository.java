package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.demo.model.Like;
import com.example.demo.model.Products;
import com.example.demo.model.Account;

public interface LikeRepository extends JpaRepository<Like, Integer> {

    @Query("SELECT w.product FROM Like w GROUP BY w.product ORDER BY COUNT(w.product) DESC LIMIT 1")
    Optional<Products> findTopByMostLiked();
    // Lấy tất cả like của 1 tài khoản (nếu có login)
    List<Like> findByAccount(Account account);

    // Mặc định sắp xếp giảm dần theo ID (mới nhất nằm đầu)
    default List<Like> findAllOrderByNewest() {
        return findAll(Sort.by(Sort.Direction.DESC, "id"));
        
    }
}
