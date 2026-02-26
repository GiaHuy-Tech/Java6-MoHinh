package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Orders;
import com.example.demo.model.Products;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Integer> {

    // Lấy 5 đơn mới nhất
    List<Orders> findTop5ByOrderByCreatedDateDesc();

    // Lấy danh sách đơn hàng theo Account ID
    List<Orders> findByAccountId_IdOrderByCreatedDateDesc(Integer accountId);
    List<Orders> findByAccountId_IdOrderByCreatedDateAsc(Integer accountId);

    // --- CÁC QUERY THỐNG KÊ ---
    
    @Query("SELECT MONTH(o.createdDate), COUNT(o) FROM Orders o GROUP BY MONTH(o.createdDate)")
    List<Object[]> countOrdersPerMonth();

    @Query("SELECT COUNT(o) FROM Orders o WHERE MONTH(o.createdDate) = :month")
    Long countOrdersByMonth(@Param("month") int month);

    @Query("SELECT COUNT(o) FROM Orders o WHERE YEAR(o.createdDate) = :year")
    Long countOrdersInYear(@Param("year") int year);

    @Query("SELECT od.productId FROM OrderDetail od GROUP BY od.productId ORDER BY SUM(od.quantity) DESC LIMIT 1")
    Optional<Products> findTopSellingProduct();

    // --- 🔥 QUAN TRỌNG CHO VNPAY 🔥 ---
    // Tìm đơn hàng dựa trên ghi chú (đã lưu mã đơn hàng vào đây)
    Optional<Orders> findByNote(String note);

    // --- TÍNH TỔNG CHI TIÊU KHÁCH HÀNG (Native Query) ---
    
    // 1. Tính tổng tiền khách đã mua (cần thiết cho logic xếp hạng thành viên)
    @Query(value = "SELECT COALESCE(SUM(CAST(total AS BIGINT)), 0) FROM orders WHERE account_id = :accountId", nativeQuery = true)
    Long sumTotalSpentByAccountId(@Param("accountId") Integer accountId);

    // 2. Đếm tổng số đơn hàng khách đã đặt
    @Query(value = "SELECT COUNT(*) FROM orders WHERE account_id = :accountId", nativeQuery = true)
    Long countByAccountId(@Param("accountId") Integer accountId);
}