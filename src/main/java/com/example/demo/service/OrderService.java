package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.*;
import com.example.demo.repository.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
	
	@Autowired
	ShippingService shippingService;   // thêm dòng này

	@Autowired
	AddressRepository addressRepository; // thêm dòng này

    @Autowired
    CartDetailRepository cartDetailRepo;

    @Autowired
    OrdersRepository orderRepo;

    @Autowired
    OrdersDetailRepository orderDetailRepo;

    public void createOrder(Account acc, String voucherCode,  Long addressId) {

        // 1. Lấy danh sách giỏ hàng trực tiếp theo Account ID
        List<CartDetail> cartList = cartDetailRepo.findByAccount_Id(acc.getId());
        
        // Nếu giỏ hàng trống thì dừng luôn
        if (cartList == null || cartList.isEmpty()) {
            return;
        }

        // 2. Tính tổng tiền gốc bằng BigDecimal
        BigDecimal rawTotal = BigDecimal.ZERO;
        for (CartDetail item : cartList) {
            BigDecimal price = item.getProduct().getPrice();
            BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
            // Cộng dồn: rawTotal = rawTotal + (price * quantity)
            rawTotal = rawTotal.add(price.multiply(quantity));
        }

        // 3. Tính tiền giảm giá (VD: voucher SALE10 giảm 10%)
        BigDecimal discount = BigDecimal.ZERO;
        if (voucherCode != null && voucherCode.equalsIgnoreCase("SALE10")) {
            // discount = rawTotal * 0.1
            discount = rawTotal.multiply(new BigDecimal("0.1")); 
        }

        // 4. Tính phí ship (Trên 1 triệu thì freeship, ngược lại 30k)
//        BigDecimal feeShip = new BigDecimal("30000");
//        if (rawTotal.compareTo(new BigDecimal("1000000")) > 0) {
//            feeShip = BigDecimal.ZERO;
//        }
        
        Map<String, Object> shippingResult = shippingService.calculate(addressId, acc);
        BigDecimal feeShip = BigDecimal.valueOf((Long) shippingResult.get("feeShip"));

        // Lấy Address entity để gắn vào đơn hàng
        Address address = addressRepository.findById(addressId).orElse(null);
        

        // 5. Tính tổng thanh toán cuối cùng: final = raw - discount + freeship
        BigDecimal finalTotal = rawTotal.subtract(discount).add(feeShip);
        
        // Đảm bảo tổng tiền không bị âm
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        // 6. Tạo đơn hàng (Lưu ý: Tên model là Orders có 's')
        Orders order = new Orders();
        order.setAccount(acc);
        order.setAddress(address); 
        order.setCreatedDate(new Date());
        order.setTotal(finalTotal);
        order.setStatus(0); // 0: Chờ xác nhận
        order.setFeeship(feeShip);
        order.setMoneyDiscounted(discount);
        order.setPaymentStatus(false);
        
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            order.setVoucherCode(voucherCode);
        }

        // Lưu đơn hàng vào DB
        orderRepo.save(order);

        // 7. Tạo chi tiết đơn hàng
        for (CartDetail cd : cartList) {
            OrderDetail od = new OrderDetail();
            od.setOrder(order);
            od.setProduct(cd.getProduct());
            od.setQuantity(cd.getQuantity());
            od.setPrice(cd.getProduct().getPrice());
            
            // Lưu chi tiết đơn hàng
            orderDetailRepo.save(od);
        }

        // 8. Xóa giỏ hàng sau khi đặt thành công
        cartDetailRepo.deleteAll(cartList);
    }
}