package com.example.demo.repository;

import com.example.demo.model.VoucherDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface VoucherDetailRepository extends JpaRepository<VoucherDetail, Long> {

    // Sử dụng accountId kiểu Integer nếu model Account của bạn dùng Integer id
    List<VoucherDetail> findByAccount_IdAndIsUsedFalse(Integer accountId);

    @Query("SELECT vd FROM VoucherDetail vd WHERE vd.account.id = :accountId " +
           "AND vd.voucher.code = :code " +
           "AND vd.isUsed = false " +
           "AND (vd.voucher.expiredAt IS NULL OR vd.voucher.expiredAt > CURRENT_TIMESTAMP)")
    Optional<VoucherDetail> findValidVoucherForAccount(@Param("accountId") Integer accountId, @Param("code") String code);
}