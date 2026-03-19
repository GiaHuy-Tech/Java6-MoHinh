package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.Address;
import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByAccountId(Integer accountId);
    Address findByAccountIdAndIsDefaultTrue(Integer accountId);

}