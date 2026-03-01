package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Membership;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Integer> {

    // Tìm theo tên hạng thành viên (optional, dùng khi cần)
    Membership findByName(String name);

}