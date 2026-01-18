package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Voucher;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

    // 1. Lấy danh sách Voucher của riêng user (Ví cá nhân)
    // SỬA: Thêm dấu gạch dưới (_) để JPA hiểu là tìm theo ID của đối tượng Account
    List<Voucher> findByAccount_IdOrderByIdDesc(Integer accountId);

    // 2. Lấy danh sách Voucher chung (Chưa thuộc về ai, còn hạn, đang active)
    // Dùng để hiển thị ở mục "Săn Voucher"
    List<Voucher> findByAccountIsNullAndActiveTrueAndExpiredAtAfter(LocalDateTime now);

    // 3. Kiểm tra xem User đã sở hữu mã này chưa (tránh spam nút nhận)
    // SỬA: Thêm dấu gạch dưới (_)
    boolean existsByAccount_IdAndCode(Integer accountId, String code);
    
    // 4. Tìm voucher gốc theo ID (để clone)
    Optional<Voucher> findByIdAndAccountIsNull(Integer id);
    List<Voucher> findByActiveTrueAndExpiredAtAfter(LocalDateTime now);

}