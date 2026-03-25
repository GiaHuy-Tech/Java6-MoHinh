package com.example.demo.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Orders;
import com.example.demo.model.Products;

public interface OrdersRepository extends JpaRepository<Orders, Integer> {

    // ===== COUNT COMPLETED =====
    @Query("""
        SELECT COUNT(o)
        FROM Orders o
        WHERE o.status = 3
        AND (:from IS NULL OR o.createdDate >= :from)
        AND (:to IS NULL OR o.createdDate < :to)
    """)
    Long countCompletedOrdersByDate(@Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    // ===== REVENUE CATEGORY =====
    @Query("""
        SELECT c.name, SUM(d.quantity * d.price)
        FROM OrderDetail d
        JOIN d.order o
        JOIN d.product p
        JOIN p.category c
        WHERE o.status = 3
        GROUP BY c.name
    """)
    List<Object[]> getRevenueByCategory();

    @Query("""
        SELECT c.name, SUM(d.quantity * d.price)
        FROM OrderDetail d
        JOIN d.order o
        JOIN d.product p
        JOIN p.category c
        WHERE o.status = 3
        AND (:from IS NULL OR o.createdDate >= :from)
        AND (:to IS NULL OR o.createdDate < :to)
        GROUP BY c.name
    """)
    List<Object[]> getRevenueByCategoryByDate(@Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);

    // ===== REVENUE MONTH =====
    @Query("""
        SELECT FUNCTION('MONTH', o.createdDate), SUM(d.quantity * d.price)
        FROM OrderDetail d
        JOIN d.order o
        WHERE FUNCTION('YEAR', o.createdDate) = :year
        AND o.status = 3
        GROUP BY FUNCTION('MONTH', o.createdDate)
        ORDER BY FUNCTION('MONTH', o.createdDate)
    """)
    List<Object[]> getRevenueByMonth(@Param("year") int year);

    // 🔥 FIX DATE RANGE
    @Query("""
        SELECT FUNCTION('MONTH', o.createdDate), SUM(d.quantity * d.price)
        FROM OrderDetail d
        JOIN d.order o
        WHERE o.status = 3
        AND (:from IS NULL OR o.createdDate >= :from)
        AND (:to IS NULL OR o.createdDate < :to)
        GROUP BY FUNCTION('MONTH', o.createdDate)
        ORDER BY FUNCTION('MONTH', o.createdDate)
    """)
    List<Object[]> getRevenueByMonthByDate(@Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    // ===== TOTAL REVENUE =====
    @Query("""
        SELECT SUM(d.quantity * d.price)
        FROM OrderDetail d
        JOIN d.order o
        WHERE o.status = 3
    """)
    Long getTotalRevenue();

    @Query("""
        SELECT SUM(d.quantity * d.price)
        FROM OrderDetail d
        JOIN d.order o
        WHERE o.status = 3
        AND (:from IS NULL OR o.createdDate >= :from)
        AND (:to IS NULL OR o.createdDate < :to)
    """)
    Long getTotalRevenueByDate(@Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to);

    // ===== TOP SELLING =====
    @Query("""
        SELECT p
        FROM OrderDetail d
        JOIN d.product p
        JOIN d.order o
        WHERE o.status = 3
        GROUP BY p
        ORDER BY SUM(d.quantity) DESC
    """)
    List<Products> findTopSellingProduct(Pageable pageable);

    // ===== ORDERS PER MONTH =====
    @Query("""
        SELECT FUNCTION('MONTH', o.createdDate), COUNT(o)
        FROM Orders o
        WHERE FUNCTION('YEAR', o.createdDate) = :year
        AND o.status = 3
        GROUP BY FUNCTION('MONTH', o.createdDate)
        ORDER BY FUNCTION('MONTH', o.createdDate)
    """)
    List<Object[]> countOrdersPerMonthByYear(@Param("year") int year);

    // 🔥 FIX DATE RANGE
    @Query("""
        SELECT FUNCTION('MONTH', o.createdDate), COUNT(o)
        FROM Orders o
        WHERE o.status = 3
        AND (:from IS NULL OR o.createdDate >= :from)
        AND (:to IS NULL OR o.createdDate < :to)
        GROUP BY FUNCTION('MONTH', o.createdDate)
        ORDER BY FUNCTION('MONTH', o.createdDate)
    """)
    List<Object[]> countOrdersPerMonthByDate(@Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);

    // ===== SUM ACCOUNT =====
    @Query("""
        SELECT SUM(o.total)
        FROM Orders o
        WHERE o.account.id = :accountId
        AND o.status = 4
    """)
    BigDecimal sumTotalByAccountAndStatus(@Param("accountId") Integer accountId);
}