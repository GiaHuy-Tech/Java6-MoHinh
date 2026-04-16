package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Account;
import com.example.demo.model.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

    List<Voucher> findByAccountOrderByIdDesc(Account account);

    List<Voucher> findByAccountIsNull();

    List<Voucher> findByMembership_IdAndAccountIsNull(Integer membershipId);

    List<Voucher> findByAccount_Id(Integer accountId);

    Optional<Voucher> findByIdAndAccountIsNull(Integer id);

    boolean existsByAccount_IdAndCode(Integer accountId, String code);

 // Thêm vào interface VoucherRepository
    List<Voucher> findByIsBirthdayTrueAndAccountIsNull();

}