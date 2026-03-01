package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.*;
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

    @Column(unique = true)
    private String email;

    private String fullName;

    private Boolean gender;

    private String password;

    private String phone;

    // Avatar ảnh đại diện
    private String avatar;

    // true = admin
    private Boolean role = false;

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