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

    // bỏ columnDefinition nvarchar
    @Column(unique = true, length = 255)
    private String email;

    // bỏ @Nationalized
    @Column(length = 255)
    private String fullName;

    private Boolean gender;

    @Column(length = 255)
    private String password;

    // đây rất dễ là cột gây lỗi convert varchar -> nchar
    @Column(length = 20)
    private String phone;

    // bỏ @Nationalized
    @Column(length = 1000)
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