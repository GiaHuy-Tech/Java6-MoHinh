package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Account;

public interface AccountRepository extends JpaRepository<Account, Integer> {

    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT a FROM Account a WHERE MONTH(a.birthDay) = :month AND DAY(a.birthDay) = :day")
    List<Account> findByBirthday(@Param("month") int month,
                                 @Param("day") int day);
    
}	