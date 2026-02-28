package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

    // Lấy voucher còn hạn + active (voucher chung)
    List<Voucher> findByActiveTrueAndExpiredAtAfter(LocalDateTime now);

    // Tìm voucher theo code
    Optional<Voucher> findByCode(String code);

    // Tìm voucher theo id
    Optional<Voucher> findById(Integer id);
}