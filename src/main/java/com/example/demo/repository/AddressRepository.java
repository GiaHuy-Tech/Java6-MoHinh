package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.Address;
import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    // Lấy danh sách địa chỉ của user
    List<Address> findByAccount_Id(Integer accountId);

    // Lấy địa chỉ mặc định
    Optional<Address> findByAccount_IdAndIsDefaultTrue(Integer accountId);

    // Tìm theo id + user (tránh hack)
    Optional<Address> findByIdAndAccount_Id(Long id, Integer accountId);
}