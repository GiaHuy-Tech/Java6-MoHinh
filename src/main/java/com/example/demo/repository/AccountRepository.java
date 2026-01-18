package com.example.demo.repository;

import java.util.List; // Cần import Optional
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Account;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    
    // --> THÊM PHƯƠNG THỨC NÀY <--
    // Tìm một tài khoản dựa vào email.
    // Sử dụng Optional<Account> để xử lý trường hợp không tìm thấy email một cách an toàn.
	 Optional<Account> findByEmail(String email);

	    boolean existsByEmail(String email);
	    boolean existsByPhone(String phone);
	 // Tìm user theo ngày và tháng sinh (bất kể năm nào)
	    @Query("SELECT a FROM Account a WHERE MONTH(a.birthday) = :month AND DAY(a.birthday) = :day")
	    List<Account> findByBirthday(@Param("month") int month, @Param("day") int day);
}