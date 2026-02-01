package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.OrderDetail;

@Repository
public interface OrdersDetailRepository extends JpaRepository<OrderDetail, Integer> {

    // ✅ Đã mua (không cần hoàn tất)
    @Query("""
        SELECT COUNT(od) > 0
        FROM OrderDetail od
        WHERE od.orders.accountId.id = :accountId
          AND od.productId.id = :productId
    """)
    boolean hasPurchased(
            @Param("accountId") Integer accountId,
            @Param("productId") Integer productId
    );

    // ✅ Đơn hoàn tất (status = 3)
    @Query("""
        SELECT COUNT(od) > 0
        FROM OrderDetail od
        WHERE od.orders.accountId.id = :accountId
          AND od.productId.id = :productId
          AND od.orders.status = 3
    """)
    boolean hasCompletedOrder(
            @Param("accountId") Integer accountId,
            @Param("productId") Integer productId
    );
}
