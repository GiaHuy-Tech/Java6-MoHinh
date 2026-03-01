package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Orders;
import com.example.demo.model.Products;

public interface OrdersRepository extends JpaRepository<Orders, Integer> {

    List<Orders> findTop5ByOrderByCreatedDateDesc();

    List<Orders> findByAccount_IdOrderByCreatedDateDesc(Integer accountId);

    List<Orders> findByAccount_IdOrderByCreatedDateAsc(Integer accountId);

    @Query("""
        SELECT MONTH(o.createdDate), COUNT(o)
        FROM Orders o
        GROUP BY MONTH(o.createdDate)
    """)
    List<Object[]> countOrdersPerMonth();

    @Query("""
        SELECT COUNT(o)
        FROM Orders o
        WHERE MONTH(o.createdDate) = :month
    """)
    Long countOrdersByMonth(@Param("month") int month);

    @Query("""
        SELECT COUNT(o)
        FROM Orders o
        WHERE YEAR(o.createdDate) = :year
    """)
    Long countOrdersInYear(@Param("year") int year);

    @Query("""
        SELECT od.product
        FROM OrderDetail od
        GROUP BY od.product
        ORDER BY SUM(od.quantity) DESC
    """)
    List<Products> findTopSellingProduct(Pageable pageable);

    Optional<Orders> findByNote(String note);
    
    @Query(value = """
        SELECT COALESCE(SUM(CAST(total AS BIGINT)),0)
        FROM orders
        WHERE account_id = :accountId
    """, nativeQuery = true)
    Long sumTotalSpentByAccountId(@Param("accountId") Integer accountId);

    @Query(value = """
        SELECT COUNT(*)
        FROM orders
        WHERE account_id = :accountId
    """, nativeQuery = true)
    Long countByAccountId(@Param("accountId") Integer accountId);
}