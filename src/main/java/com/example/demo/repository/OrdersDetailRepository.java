package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.demo.model.OrderDetail;

@Repository
public interface OrdersDetailRepository extends JpaRepository<OrderDetail, Integer> {

    // Kiểm tra đã mua và đơn thành công (status = 3 hoặc trạng thái hoàn thành của bạn)
    @Query("SELECT COUNT(od) > 0 FROM OrderDetail od " +
           "WHERE od.orders.accountId.id = :accountId " +
           "AND od.productId.id = :productId " +
           "AND od.orders.status = 3") 
    boolean existsByAccountAndProduct(@Param("accountId") Integer accountId, 
                                      @Param("productId") Integer productId);
}