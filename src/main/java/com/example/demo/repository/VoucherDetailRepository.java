package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.VoucherDetail;

public interface VoucherDetailRepository 
        extends JpaRepository<VoucherDetail, Long> {

    @Query("""
           SELECT vd
           FROM VoucherDetail vd
           WHERE vd.account.id = :accountId
           AND vd.voucher.active = true
           AND vd.voucher.expiredAt > :now
           AND vd.isUsed = false
           """)
    List<VoucherDetail> findAvailableVouchers(
            @Param("accountId") Integer accountId,
            @Param("now") LocalDateTime now
    );
}