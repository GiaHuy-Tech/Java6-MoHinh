package com.example.demo.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Orders;
import com.example.demo.model.Products;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Integer> {

    // Lấy 5 đơn hàng mới nhất
    List<Orders> findTop5ByOrderByCreatedDateDesc();

    // Doanh thu tháng hiện tại
    @Query("SELECT SUM(o.total) FROM Orders o WHERE MONTH(o.createdDate) = MONTH(CURRENT_DATE) AND YEAR(o.createdDate) = YEAR(CURRENT_DATE)")
    Double getCurrentMonthRevenue();

    // Doanh thu cả năm hiện tại
    @Query("SELECT SUM(o.total) FROM Orders o WHERE YEAR(o.createdDate) = YEAR(CURRENT_DATE)")
    Double getYearRevenue();

    // Doanh thu theo tháng trong khoảng từ fromDate
    @Query("SELECT MONTH(o.createdDate), SUM(o.total) FROM Orders o WHERE o.createdDate >= :fromDate GROUP BY MONTH(o.createdDate)")
    List<Object[]> getRevenueLastMonths(Date fromDate);

    // Danh sách đơn hàng theo tài khoản (mới nhất trước)
    List<Orders> findByAccountId_IdOrderByCreatedDateDesc(Integer accountId);

    // Tổng doanh thu theo từng tháng (toàn bộ dữ liệu)
    @Query("SELECT MONTH(o.createdDate), SUM(o.total) FROM Orders o GROUP BY MONTH(o.createdDate)")
    List<Object[]> revenuePerMonth();

    // ✅ Doanh thu theo tháng (sửa field)
    @Query("SELECT SUM(o.total) FROM Orders o WHERE MONTH(o.createdDate) = ?1")
    Double getRevenueByMonth(int month);

    // ✅ Doanh thu theo năm (sửa field)
    @Query("SELECT SUM(o.total) FROM Orders o WHERE YEAR(o.createdDate) = ?1")
    Double getRevenueByYear(int year);

    // ✅ Sản phẩm bán chạy nhất
    @Query("SELECT od.productId FROM OrderDetail od GROUP BY od.productId ORDER BY SUM(od.quantity) DESC LIMIT 1")
    Optional<Products> findTopSellingProduct();

    // ✅ Doanh thu theo nhãn tháng (ví dụ 'Tháng 5')
    @Query("SELECT SUM(o.total) FROM Orders o WHERE CONCAT('Tháng ', MONTH(o.createdDate)) = ?1")
    Double getRevenueByMonthLabel(String label);
}
