package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Column(columnDefinition = "nvarchar(255)")
    private String detail;

    private Boolean isDefault = false;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;
}