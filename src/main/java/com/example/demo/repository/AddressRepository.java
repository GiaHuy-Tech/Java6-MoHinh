package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.Address;
import java.util.List;
import java.util.Optional; // Thêm thư viện này

public interface AddressRepository extends JpaRepository<Address, Long> {
    
    // --- GIỮ NGUYÊN CÁC PHƯƠNG THỨC CŨ ---
    List<Address> findByAccountId(Integer accountId);
    Address findByAccountIdAndIsDefaultTrue(Integer accountId);

    // --- THÊM PHƯƠNG THỨC MỚI ĐỂ HẾT BÁO ĐỎ Ở CONTROLLER ---
    // Việc thêm dấu gạch dưới (_) giúp JPA hiểu rõ hơn về liên kết giữa Address và Account
    Optional<Address> findByAccount_IdAndIsDefaultTrue(Integer accountId);

}