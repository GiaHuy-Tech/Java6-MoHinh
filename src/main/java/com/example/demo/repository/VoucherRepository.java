package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

    List<Voucher> findByActiveTrueAndExpiredAtAfter(LocalDateTime now);
}
