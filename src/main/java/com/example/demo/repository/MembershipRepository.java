package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Membership;


@Repository
public interface MembershipRepository extends JpaRepository<Membership, Integer> {

    // Tìm theo tên hạng thành viên (optional, dùng khi cần)
    Membership findByName(String name);
 // Lấy thống kê: Tên hạng + Số lượng account đang sở hữu hạng đó
    @Query("SELECT m.name, COUNT(a.id) " +
           "FROM Membership m LEFT JOIN m.accounts a " +
           "GROUP BY m.id, m.name")
    List<Object[]> countUsersByMembership();

}