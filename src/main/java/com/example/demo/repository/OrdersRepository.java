package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.demo.model.Orders;
import com.example.demo.model.Products;
import com.example.demo.model.OrderDetail;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Integer> { // Sửa Long -> Integer

    // --- CÁC HÀM MỚI THÊM CHO CONTROLLER ---
    // Tìm đơn hàng theo Account ID và sắp xếp ngày tạo giảm dần (Mới nhất)
    List<Orders> findByAccount_IdOrderByCreatedDateDesc(Integer accountId);

    // Tìm đơn hàng theo Account ID và sắp xếp ngày tạo tăng dần (Cũ nhất)
    List<Orders> findByAccount_IdOrderByCreatedDateAsc(Integer accountId);
    // ---------------------------------------

    // 1. Đếm tổng số đơn hàng đã hoàn tất (Status = 3)
    @Query("SELECT COUNT(o) FROM Orders o WHERE o.status = 3")
    long countCompletedOrders();

    // 2. Thống kê doanh thu theo Danh mục (Dựa trên đơn hoàn tất status = 3)
    // Sử dụng d.price (giá lúc mua) thay vì p.price (giá hiện tại) để chính xác doanh thu thực tế
    @Query("SELECT c.name, SUM(d.quantity * d.price) " +
           "FROM OrderDetail d " + 
           "JOIN d.order o " +
           "JOIN d.product p " +
           "JOIN p.category c " +
           "WHERE o.status = 3 " + 
           "GROUP BY c.name")
    List<Object[]> getRevenueByCategory();

    // 3. Thống kê doanh thu theo từng tháng của năm chỉ định
    @Query("SELECT MONTH(o.createdDate), SUM(d.quantity * d.price) " +
           "FROM OrderDetail d " + 
           "JOIN d.order o " +
           "WHERE YEAR(o.createdDate) = ?1 AND o.status = 3 " + 
           "GROUP BY MONTH(o.createdDate) " +
           "ORDER BY MONTH(o.createdDate)")
    List<Object[]> getRevenueByMonth(int year);
    
    // 4. Tổng doanh thu toàn thời gian
    @Query("SELECT SUM(d.quantity * d.price) " +
           "FROM OrderDetail d " + 
           "JOIN d.order o " +
           "WHERE o.status = 3")
    Long getTotalRevenue();
    
    // 5. Top bán chạy (Dựa trên số lượng đã bán trong các đơn hoàn tất)
    @Query("SELECT p FROM OrderDetail d " +
           "JOIN d.product p " +
           "JOIN d.order o " +
           "WHERE o.status = 3 " +
           "GROUP BY p " +
           "ORDER BY SUM(d.quantity) DESC")
    List<Products> findTopSellingProduct(org.springframework.data.domain.Pageable pageable);

    // 6. Đếm số đơn theo tháng trong năm nay
    @Query("SELECT MONTH(o.createdDate), COUNT(o) " +
           "FROM Orders o " +
           "WHERE YEAR(o.createdDate) = YEAR(CURRENT_DATE) AND o.status = 3 " +
           "GROUP BY MONTH(o.createdDate) " +
           "ORDER BY MONTH(o.createdDate)")
    List<Object[]> countOrdersPerMonth();
}