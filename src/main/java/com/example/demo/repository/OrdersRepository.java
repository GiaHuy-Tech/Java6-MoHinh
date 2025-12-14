package com.example.demo.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Orders;
import com.example.demo.model.Products;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Integer> {

    //Lấy 5 đơn hàng mới nhất
    List<Orders> findTop5ByOrderByCreatedDateDesc();

    //Danh sách đơn hàng theo tài khoản (Mới nhất trước)
    List<Orders> findByAccountId_IdOrderByCreatedDateDesc(Integer accountId);

    //Danh sách đơn hàng theo tài khoản (Cũ nhất trước)
    List<Orders> findByAccountId_IdOrderByCreatedDateAsc(Integer accountId);

    //Số đơn theo từng tháng (cho bảng)
    @Query("SELECT MONTH(o.createdDate), COUNT(o) FROM Orders o GROUP BY MONTH(o.createdDate)")
    List<Object[]> countOrdersPerMonth();

    //Số đơn theo tháng (cho biểu đồ)
    @Query("SELECT COUNT(o) FROM Orders o WHERE MONTH(o.createdDate) = :month")
    Long countOrdersByMonth(int month);

    //Số đơn trong tháng hiện tại
    @Query("SELECT COUNT(o) FROM Orders o WHERE MONTH(o.createdDate) = :month")
    Long countOrdersInMonth(int month);

    //Số đơn trong năm hiện tại
    @Query("SELECT COUNT(o) FROM Orders o WHERE YEAR(o.createdDate) = :year")
    Long countOrdersInYear(int year);

    //Sản phẩm bán chạy nhất
    @Query("SELECT od.productId FROM OrderDetail od GROUP BY od.productId ORDER BY SUM(od.quantity) DESC LIMIT 1")
    Optional<Products> findTopSellingProduct();
}