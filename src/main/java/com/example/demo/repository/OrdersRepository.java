package com.example.demo.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Orders;
import com.example.demo.model.Products;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Integer> {

    // 1. Tìm đơn hàng theo tài khoản (Dùng cho trang Orders)
    List<Orders> findByAccount_IdOrderByCreatedDateDesc(Integer accountId);
    List<Orders> findByAccount_IdOrderByCreatedDateAsc(Integer accountId);

    // 2. Đếm số đơn hàng của tài khoản (Dùng cho trang Account)
    @Query("SELECT COUNT(o) FROM Orders o WHERE o.account.id = :accountId")
    Long countByAccountId(@Param("accountId") Integer accountId);

    // 3. Tổng chi tiêu của tài khoản (Hạng thành viên)
    @Query("SELECT SUM(o.total) FROM Orders o WHERE o.account.id = :accountId AND o.status = 4")
    BigDecimal sumTotalByAccountAndStatus(@Param("accountId") Integer accountId);

    // ========================================================================
    // CÁC HÀM THỐNG KÊ (Dùng cho StatsController)
    // ========================================================================

    // 4. Đếm số đơn hàng hoàn tất theo khoảng ngày (Dòng 61 Controller)
    @Query("""
        SELECT COUNT(o) FROM Orders o 
        WHERE o.status >= 3 
        AND (:from IS NULL OR o.createdDate >= :from) 
        AND (:to IS NULL OR o.createdDate <= :to)
    """)
    Long countCompletedOrdersByDate(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 5. Đếm số đơn hàng theo tháng trong năm (Dòng 62 & 116 Controller)
    @Query("""
        SELECT FUNCTION('MONTH', o.createdDate), COUNT(o) 
        FROM Orders o 
        WHERE FUNCTION('YEAR', o.createdDate) = :year AND o.status >= 3 
        GROUP BY FUNCTION('MONTH', o.createdDate) 
        ORDER BY FUNCTION('MONTH', o.createdDate)
    """)
    List<Object[]> countOrdersPerMonthByYear(@Param("year") int year);

    // 6. Đếm số đơn hàng theo tháng trong khoảng ngày (Dòng 115 Controller)
    @Query("""
        SELECT FUNCTION('MONTH', o.createdDate), COUNT(o) 
        FROM Orders o 
        WHERE o.status >= 3 
        AND (:from IS NULL OR o.createdDate >= :from) 
        AND (:to IS NULL OR o.createdDate <= :to) 
        GROUP BY FUNCTION('MONTH', o.createdDate) 
        ORDER BY FUNCTION('MONTH', o.createdDate)
    """)
    List<Object[]> countOrdersPerMonthByDate(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 7. Tổng doanh thu theo khoảng ngày (Dòng 74 Controller)
    @Query("""
        SELECT SUM(d.quantity * d.price) 
        FROM OrderDetail d JOIN d.order o 
        WHERE o.status >= 3 
        AND (:from IS NULL OR o.createdDate >= :from) 
        AND (:to IS NULL OR o.createdDate <= :to)
    """)
    Long getTotalRevenueByDate(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 8. Doanh thu theo tháng trong khoảng ngày (Dòng 95 Controller)
    @Query("""
        SELECT FUNCTION('MONTH', o.createdDate), SUM(d.quantity * d.price) 
        FROM OrderDetail d JOIN d.order o 
        WHERE o.status >= 3 
        AND (:from IS NULL OR o.createdDate >= :from) 
        AND (:to IS NULL OR o.createdDate <= :to) 
        GROUP BY FUNCTION('MONTH', o.createdDate) 
        ORDER BY FUNCTION('MONTH', o.createdDate)
    """)
    List<Object[]> getRevenueByMonthByDate(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 9. Doanh thu theo tháng trong năm (Dòng 75 & 96 Controller)
    @Query("""
        SELECT FUNCTION('MONTH', o.createdDate), SUM(d.quantity * d.price) 
        FROM OrderDetail d JOIN d.order o 
        WHERE FUNCTION('YEAR', o.createdDate) = :year AND o.status >= 3 
        GROUP BY FUNCTION('MONTH', o.createdDate) 
        ORDER BY FUNCTION('MONTH', o.createdDate)
    """)
    List<Object[]> getRevenueByMonth(@Param("year") int year);

    // 10. Doanh thu theo danh mục (Dòng 82 Controller)
    @Query("""
        SELECT c.name, SUM(d.quantity * d.price) 
        FROM OrderDetail d JOIN d.order o JOIN d.product p JOIN p.category c 
        WHERE o.status >= 3 
        GROUP BY c.name
    """)
    List<Object[]> getRevenueByCategory();

    // 11. Doanh thu theo danh mục & ngày (Dòng 81 Controller)
    @Query("""
        SELECT c.name, SUM(d.quantity * d.price) 
        FROM OrderDetail d JOIN d.order o JOIN d.product p JOIN p.category c 
        WHERE o.status >= 3 
        AND (:from IS NULL OR o.createdDate >= :from) 
        AND (:to IS NULL OR o.createdDate <= :to) 
        GROUP BY c.name
    """)
    List<Object[]> getRevenueByCategoryByDate(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 12. Top sản phẩm bán chạy (Dòng 108 Controller)
    @Query("""
        SELECT p 
        FROM OrderDetail d JOIN d.product p JOIN d.order o 
        WHERE o.status >= 3 
        GROUP BY p 
        ORDER BY SUM(d.quantity) DESC
    """)
    List<Products> findTopSellingProduct(Pageable pageable);
}