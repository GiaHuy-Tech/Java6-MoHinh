	package com.example.demo.dto;

	import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public class CheckoutData {
	    private String address;
	    private String phone;
	    private String paymentMethod;
	    private int totalAmount; // Tiền hàng
	    private int shippingFee; // Phí ship
	    private int finalTotal;  // Tổng thanh toán
	    private String tempOrderCode; // Mã đơn tạm thời
	}