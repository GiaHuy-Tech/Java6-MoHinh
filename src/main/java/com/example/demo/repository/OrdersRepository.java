package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.demo.model.Orders;
import com.example.demo.model.Products;
import com.example.demo.model.OrderDetail; // Import đúng class này

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Long> {

    // 1. Đếm tổng số đơn hàng đã hoàn tất
    @Query("SELECT COUNT(o) FROM Orders o WHERE o.status = 4")
    long countCompletedOrders();

    // 2. Thống kê doanh thu theo Danh mục
    // SỬA: OrderDetails -> OrderDetail
    @Query("SELECT c.name, SUM(d.quantity * p.price) " +
           "FROM OrderDetail d " + 
           "JOIN d.order o " +
           "JOIN d.product p " +
           "JOIN p.category c " +
           "WHERE o.status = 4 " + 
           "GROUP BY c.name")
    List<Object[]> getRevenueByCategory();

    // 3. Thống kê doanh thu theo từng tháng
    // SỬA: OrderDetails -> OrderDetail
    @Query("SELECT MONTH(o.createdDate), SUM(d.quantity * p.price) " +
           "FROM OrderDetail d " + 
           "JOIN d.order o " +
           "JOIN d.product p " +
           "WHERE YEAR(o.createdDate) = ?1 AND o.status = 4 " + 
           "GROUP BY MONTH(o.createdDate) " +
           "ORDER BY MONTH(o.createdDate)")
    List<Object[]> getRevenueByMonth(int year);
    
    // 4. Tổng doanh thu toàn thời gian
    // SỬA: OrderDetails -> OrderDetail
    @Query("SELECT SUM(d.quantity * p.price) " +
           "FROM OrderDetail d " + 
           "JOIN d.order o " +
           "JOIN d.product p " +
           "WHERE o.status = 4")
    Long getTotalRevenue();
    
    // 5. Top bán chạy
    // SỬA: OrderDetails -> OrderDetail
    @Query("SELECT p FROM OrderDetail d JOIN d.product p GROUP BY p ORDER BY SUM(d.quantity) DESC")
    List<Products> findTopSellingProduct(org.springframework.data.domain.Pageable pageable);

    // 6. Đếm số đơn theo tháng (Dùng entity Orders nên không cần sửa)
    @Query("SELECT MONTH(o.createdDate), COUNT(o) " +
           "FROM Orders o " +
           "WHERE YEAR(o.createdDate) = YEAR(CURRENT_DATE) AND o.status = 4 " +
           "GROUP BY MONTH(o.createdDate) " +
           "ORDER BY MONTH(o.createdDate)")
    List<Object[]> countOrdersPerMonth();
}