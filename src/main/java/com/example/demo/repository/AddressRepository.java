package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.Address;
import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Integer> {
    List<Address> findByAccountId(Integer accountId);
}