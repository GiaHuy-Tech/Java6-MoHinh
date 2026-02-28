package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {

    Optional<Address> findFirstByAccount_IdAndIsDefaultTrue(Integer accountId);
}