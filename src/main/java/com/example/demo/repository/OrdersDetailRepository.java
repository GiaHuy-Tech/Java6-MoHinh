package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.OrderDetail;

@Repository
public interface OrdersDetailRepository extends JpaRepository<OrderDetail, Integer> {

    @Query("""
        SELECT COUNT(od) > 0
        FROM OrderDetail od
        WHERE od.order.account.id = :accountId
          AND od.product.id = :productId
    """)
    boolean hasPurchased(
            @Param("accountId") Integer accountId,
            @Param("productId") Integer productId
    );

    @Query("""
        SELECT COUNT(od) > 0
        FROM OrderDetail od
        WHERE od.order.account.id = :accountId
          AND od.product.id = :productId
          AND od.order.status = 3
    """)
    boolean hasCompletedOrder(
            @Param("accountId") Integer accountId,
            @Param("productId") Integer productId
    );
}