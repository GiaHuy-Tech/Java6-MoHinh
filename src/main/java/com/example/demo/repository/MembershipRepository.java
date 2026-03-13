package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Membership;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Integer> {

    // Tìm membership theo tên (dùng cho AccountController)
    Optional<Membership> findByName(String name);

    // Thống kê số user theo từng membership
    @Query("""
        SELECT m.name, COUNT(a.id)
        FROM Membership m
        LEFT JOIN m.accounts a
        GROUP BY m.id, m.name
    """)
    List<Object[]> countUsersByMembership();

}