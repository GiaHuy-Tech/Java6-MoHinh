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

	// ===================== AUTO TASK (FIX LỖI) =====================
	List<Orders> findByStatusAndCreatedDateBefore(int status, LocalDateTime date);

	// 1. Tìm đơn hàng theo tài khoản
	List<Orders> findByAccount_IdOrderByCreatedDateDesc(Integer accountId);

	List<Orders> findByAccount_IdOrderByCreatedDateAsc(Integer accountId);

	// 2. Đếm số đơn hàng
	@Query("SELECT COUNT(o) FROM Orders o WHERE o.account.id = :accountId")
	Long countByAccountId(@Param("accountId") Integer accountId);

	// 3. Tổng chi tiêu (FIX OVERFLOW)
	@Query("""
			    SELECT SUM(CAST(o.total AS big_decimal))
			    FROM Orders o
			    WHERE o.account.id = :accountId AND o.status = 4
			""")
	BigDecimal sumTotalByAccountAndStatus(@Param("accountId") Integer accountId);

	// ===================== STATS =====================

	@Query("""
			    SELECT COUNT(o) FROM Orders o
			    WHERE o.status >= 3
			    AND (:from IS NULL OR o.createdDate >= :from)
			    AND (:to IS NULL OR o.createdDate <= :to)
			""")
	Long countCompletedOrdersByDate(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	@Query("""
			    SELECT FUNCTION('MONTH', o.createdDate), COUNT(o)
			    FROM Orders o
			    WHERE FUNCTION('YEAR', o.createdDate) = :year AND o.status >= 3
			    GROUP BY FUNCTION('MONTH', o.createdDate)
			    ORDER BY FUNCTION('MONTH', o.createdDate)
			""")
	List<Object[]> countOrdersPerMonthByYear(@Param("year") int year);

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

	// ✅ FIX OVERFLOW
	@Query("""
			    SELECT SUM(CAST(d.quantity * d.price AS big_decimal))
			    FROM OrderDetail d JOIN d.order o
			    WHERE o.status >= 3
			    AND (:from IS NULL OR o.createdDate >= :from)
			    AND (:to IS NULL OR o.createdDate <= :to)
			""")
	BigDecimal getTotalRevenueByDate(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	// ✅ FIX OVERFLOW
	@Query("""
			    SELECT FUNCTION('MONTH', o.createdDate),
			           SUM(CAST(d.quantity * d.price AS big_decimal))
			    FROM OrderDetail d JOIN d.order o
			    WHERE o.status >= 3
			    AND (:from IS NULL OR o.createdDate >= :from)
			    AND (:to IS NULL OR o.createdDate <= :to)
			    GROUP BY FUNCTION('MONTH', o.createdDate)
			    ORDER BY FUNCTION('MONTH', o.createdDate)
			""")
	List<Object[]> getRevenueByMonthByDate(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	// ✅ FIX OVERFLOW
	@Query("""
			    SELECT FUNCTION('MONTH', o.createdDate),
			           SUM(CAST(d.quantity * d.price AS big_decimal))
			    FROM OrderDetail d JOIN d.order o
			    WHERE FUNCTION('YEAR', o.createdDate) = :year AND o.status >= 3
			    GROUP BY FUNCTION('MONTH', o.createdDate)
			    ORDER BY FUNCTION('MONTH', o.createdDate)
			""")
	List<Object[]> getRevenueByMonth(@Param("year") int year);

	// ✅ FIX OVERFLOW
	@Query("""
			    SELECT c.name, SUM(CAST(d.quantity * d.price AS big_decimal))
			    FROM OrderDetail d JOIN d.order o JOIN d.product p JOIN p.category c
			    WHERE o.status >= 3
			    GROUP BY c.name
			""")
	List<Object[]> getRevenueByCategory();

	// ✅ FIX OVERFLOW
	@Query("""
			    SELECT c.name, SUM(CAST(d.quantity * d.price AS big_decimal))
			    FROM OrderDetail d JOIN d.order o JOIN d.product p JOIN p.category c
			    WHERE o.status >= 3
			    AND (:from IS NULL OR o.createdDate >= :from)
			    AND (:to IS NULL OR o.createdDate <= :to)
			    GROUP BY c.name
			""")
	List<Object[]> getRevenueByCategoryByDate(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	@Query("""
			    SELECT p
			    FROM OrderDetail d JOIN d.product p JOIN d.order o
			    WHERE o.status >= 3
			    GROUP BY p
			    ORDER BY SUM(d.quantity) DESC
			""")
	List<Products> findTopSellingProduct(Pageable pageable);
}