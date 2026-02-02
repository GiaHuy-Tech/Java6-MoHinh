package com.example.demo.model;

import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cart")
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Temporal(TemporalType.TIMESTAMP)
    @PastOrPresent(message = "Ngày tạo giỏ hàng không hợp lệ")
    Date createdDate;

    @ManyToOne
    @JoinColumn(name = "accountId")
    @NotNull(message = "Giỏ hàng phải thuộc về một tài khoản")
    Account account;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL)
    List<CartDetail> cartDetails;
}
