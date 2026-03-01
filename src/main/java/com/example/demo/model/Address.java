package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(columnDefinition = "nvarchar(255)")
    String fullName;

    String phone;

    @Column(columnDefinition = "nvarchar(255)")
    String street;

    @Column(columnDefinition = "nvarchar(255)")
    String ward;

    @Column(columnDefinition = "nvarchar(255)")
    String district;

    @Column(columnDefinition = "nvarchar(255)")
    String city;

    Boolean isDefault = true;

    @ManyToOne
    @JoinColumn(name = "account_id")
    Account account;
}