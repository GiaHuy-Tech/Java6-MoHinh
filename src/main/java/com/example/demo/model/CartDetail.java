package com.example.demo.model;

import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.*;
import lombok.Data;

@SuppressWarnings("serial")
@Data
@Entity
@Table(name = "cart_detail")
public class CartDetail implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private Integer quantity;

	@Column(name = "createdate")
	@Temporal(TemporalType.DATE)
	private Date createDate;

	@ManyToOne
	@JoinColumn(name = "account_id")
	private Account account;

	@ManyToOne
	@JoinColumn(name = "product_id")
	private Products product;
}