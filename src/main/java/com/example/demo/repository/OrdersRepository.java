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

    List<Orders> findTop5ByOrderByCreatedDateDesc();

    // Lưu ý: Trong Entity Orders biến tên là "accountId" (chữ d viết thường)
    List<Orders> findByAccountId_IdOrderByCreatedDateDesc(Integer accountId);
    List<Orders> findByAccountId_IdOrderByCreatedDateAsc(Integer accountId);

    @Query("SELECT MONTH(o.createdDate), COUNT(o) FROM Orders o GROUP BY MONTH(o.createdDate)")
    List<Object[]> countOrdersPerMonth();

    @Query("SELECT COUNT(o) FROM Orders o WHERE MONTH(o.createdDate) = :month")
    Long countOrdersByMonth(@Param("month") int month);

    @Query("SELECT COUNT(o) FROM Orders o WHERE MONTH(o.createdDate) = :month")
    Long countOrdersInMonth(@Param("month") int month);

    @Query("SELECT COUNT(o) FROM Orders o WHERE YEAR(o.createdDate) = :year")
    Long countOrdersInYear(@Param("year") int year);

    @Query("SELECT od.productId FROM OrderDetail od GROUP BY od.productId ORDER BY SUM(od.quantity) DESC LIMIT 1")
    Optional<Products> findTopSellingProduct();

    Optional<Orders> findByNote(String note);

    // --- 🔥 CÁC HÀM ĐÃ SỬA LẠI CHO KHỚP ENTITY CỦA BẠN 🔥 ---

    // 1. Tính tổng tiền: Cột trong DB là "total" (dựa theo biến int total;)
    //    Cột khóa ngoại là "accountId" (dựa theo @JoinColumn(name = "accountId"))
    //    Tham số đầu vào là Integer cho khớp với ID của Account
    @Query(value = "SELECT COALESCE(SUM(CAST(total AS BIGINT)), 0) FROM orders WHERE account_id = :accountId", nativeQuery = true)
    Long sumTotalSpentByAccountId(@Param("accountId") Integer accountId);
    // 2. Đếm số đơn (Sửa WHERE accountId -> WHERE account_id)
    @Query(value = "SELECT COUNT(*) FROM orders WHERE account_id = :accountId", nativeQuery = true)
    Long countByAccountId(@Param("accountId") Integer accountId);
}