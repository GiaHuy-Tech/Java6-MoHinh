package com.example.demo.repository;

import java.util.Date;
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

    List<Orders> findByAccount_IdOrderByCreatedDateDesc(Integer accountId);
    List<Orders> findByAccount_IdOrderByCreatedDateAsc(Integer accountId);

    // ===== Đếm đơn hoàn tất =====
    @Query("SELECT COUNT(o) FROM Orders o WHERE o.status = 3")
    long countCompletedOrders();

    // ===== Doanh thu theo danh mục =====
    @Query("SELECT c.name, SUM(d.quantity * d.price) " +
           "FROM OrderDetail d " +
           "JOIN d.order o " +
           "JOIN d.product p " +
           "JOIN p.category c " +
           "WHERE o.status = 3 " +
           "GROUP BY c.name")
    List<Object[]> getRevenueByCategory();

    // ===== Doanh thu theo tháng (theo năm) =====
    @Query("SELECT MONTH(o.createdDate), SUM(d.quantity * d.price) " +
           "FROM OrderDetail d " +
           "JOIN d.order o " +
           "WHERE YEAR(o.createdDate) = :year AND o.status = 3 " +
           "GROUP BY MONTH(o.createdDate) " +
           "ORDER BY MONTH(o.createdDate)")
    List<Object[]> getRevenueByMonth(@Param("year") int year);

    // ===== Tổng doanh thu =====
    @Query("SELECT SUM(d.quantity * d.price) " +
           "FROM OrderDetail d " +
           "JOIN d.order o " +
           "WHERE o.status = 3")
    Long getTotalRevenue();

    // ===== Top bán chạy =====
    @Query("SELECT p FROM OrderDetail d " +
           "JOIN d.product p " +
           "JOIN d.order o " +
           "WHERE o.status = 3 " +
           "GROUP BY p " +
           "ORDER BY SUM(d.quantity) DESC")
    List<Products> findTopSellingProduct(Pageable pageable);

    // ===== Đếm đơn theo tháng (lọc năm) =====
    @Query("SELECT MONTH(o.createdDate), COUNT(o) " +
           "FROM Orders o " +
           "WHERE YEAR(o.createdDate) = :year AND o.status = 3 " +
           "GROUP BY MONTH(o.createdDate) " +
           "ORDER BY MONTH(o.createdDate)")
    List<Object[]> countOrdersPerMonthByYear(@Param("year") int year);

    List<Orders> findByStatusAndCreatedDateBefore(Integer status, Date date);
}