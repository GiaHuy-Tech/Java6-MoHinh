package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.annotations.Nationalized; // THÊM DÒNG NÀY

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "accounts")
@Data
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Boolean active = true;
    private LocalDate birthDay;

    @Column(unique = true, columnDefinition = "nvarchar(255)")
    private String email;

    @Nationalized
    @Column(columnDefinition = "nvarchar(255)")
    private String fullName;

    private Boolean gender;
//
    @Column(columnDefinition = "nvarchar(255)")
    private String password;

    @Column(columnDefinition = "nvarchar(20)")
    private String phone;

    @Nationalized
    @Column(columnDefinition = "nvarchar(MAX)")
    private String avatar;

    private Boolean role = false;

    @Column(name = "total_spending", precision = 18, scale = 2)
    private BigDecimal totalSpending = BigDecimal.ZERO;

    @ManyToOne
    @JoinColumn(name = "membership_id")
    private Membership membership;

    @OneToMany(mappedBy = "account")
    private List<Address> addresses;

    @OneToMany(mappedBy = "account")
    private List<Orders> orders;

    @OneToMany(mappedBy = "account")
    private List<CartDetail> cartDetails;

    @OneToMany(mappedBy = "account")
    private List<Like> likes;

    @OneToMany(mappedBy = "account")
    private List<Comment> comments;
}