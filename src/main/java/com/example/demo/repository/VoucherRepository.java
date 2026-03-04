package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    
    // Tìm các voucher của một user cụ thể
    List<Voucher> findByAccount_IdOrderByIdDesc(Integer accountId);
    
    // Tìm các voucher chung (account = null), đang active và chưa hết hạn
    List<Voucher> findByAccountIsNullAndActiveTrueAndExpiredAtAfter(LocalDateTime time);
    
    // Tìm 1 voucher gốc (để user bấm lưu)
    Optional<Voucher> findByIdAndAccountIsNull(Integer id);
    
    // Kiểm tra xem user đã lưu mã code này chưa
    boolean existsByAccount_IdAndCode(Integer accountId, String code);

    // ✅ THÊM: dùng cho checkout
    Optional<Voucher> findByCode(String code);
}