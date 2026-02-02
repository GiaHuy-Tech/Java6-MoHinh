package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

    // ===== VOUCHER CỦA USER =====
    List<Voucher> findByAccount_IdOrderByIdDesc(Integer accountId);

    boolean existsByAccount_IdAndCode(Integer accountId, String code);
    List<Voucher> findByAccount_IdAndActiveTrueAndExpiredAtAfter(
            Integer accountId,
            LocalDateTime now
    );
    // ===== VOUCHER CHUNG =====
    List<Voucher> findByAccountIsNullAndActiveTrueAndExpiredAtAfter(LocalDateTime now);

    // 🔥 DÙNG CHO PRODUCT DETAIL
    List<Voucher> findByExpiredAtAfterAndActiveTrue(LocalDateTime now);

    Optional<Voucher> findByIdAndAccountIsNull(Integer id);
}
