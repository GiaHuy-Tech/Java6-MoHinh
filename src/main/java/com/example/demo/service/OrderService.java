package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.*;
import com.example.demo.repository.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    CartDetailRepository cartDetailRepo;

    @Autowired
    OrdersRepository orderRepo;

    @Autowired
    OrdersDetailRepository orderDetailRepo;
    
    @Autowired
    ProductRepository productRepo; // Thêm repo này để trừ kho

    // =====================================================
    // 1. TẠO ĐƠN HÀNG (Dùng khi khách nhấn Thanh toán)
    // =====================================================
    @Transactional
    public void createOrder(Account acc, String voucherCode) {

        List<CartDetail> cartList = cartDetailRepo.findByAccount_Id(acc.getId());
        
        if (cartList == null || cartList.isEmpty()) {
            return;
        }

        BigDecimal rawTotal = BigDecimal.ZERO;
        for (CartDetail item : cartList) {
            BigDecimal price = item.getProduct().getPrice();
            BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
            rawTotal = rawTotal.add(price.multiply(quantity));
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (voucherCode != null && voucherCode.equalsIgnoreCase("SALE10")) {
            discount = rawTotal.multiply(new BigDecimal("0.1")); 
        }

        BigDecimal feeShip = new BigDecimal("30000");
        if (rawTotal.compareTo(new BigDecimal("1000000")) > 0) {
            feeShip = BigDecimal.ZERO;
        }

        BigDecimal finalTotal = rawTotal.subtract(discount).add(feeShip);
        
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        Orders order = new Orders();
        order.setAccount(acc);
        order.setCreatedDate(new Date());
        order.setTotal(finalTotal);
        order.setStatus(0); // 0: Chờ xác nhận
        order.setFeeship(feeShip);
        order.setMoneyDiscounted(discount);
        order.setPaymentStatus(false);
        
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            order.setVoucherCode(voucherCode);
        }

        orderRepo.save(order);

        for (CartDetail cd : cartList) {
            OrderDetail od = new OrderDetail();
            od.setOrder(order);
            od.setProduct(cd.getProduct());
            od.setQuantity(cd.getQuantity());
            od.setPrice(cd.getProduct().getPrice());
            orderDetailRepo.save(od);
        }

        cartDetailRepo.deleteAll(cartList);
    }

    // =====================================================
    // 2. HOÀN TẤT ĐƠN HÀNG (Dùng cho cả bấm tay & tự động)
    // =====================================================
    @Transactional
    public void completeOrder(Orders order) {
        // Chỉ xử lý nếu đơn chưa Hoàn tất (4) và chưa Hủy (5)
        if (order.getStatus() < 4) {
            
            // A. Trừ kho sản phẩm
            List<OrderDetail> details = order.getOrderDetails();
            if (details != null) {
                for (OrderDetail detail : details) {
                    Products product = detail.getProduct();
                    if (product != null) {
                        // Tính số lượng mới (không để âm)
                        int newQty = Math.max(0, product.getQuantity() - detail.getQuantity());
                        product.setQuantity(newQty);
                        
                        // Nếu hết hàng thì ngừng kinh doanh sản phẩm đó
                        if (newQty <= 0) {
                            product.setAvailable(false);
                        }
                        productRepo.save(product);
                    }
                }
            }

            // B. Cập nhật trạng thái đơn hàng
            order.setStatus(4); // 4 = Hoàn tất
            order.setPaymentStatus(true); // Đã nhận tiền
            orderRepo.save(order);
            
            System.out.println("Đơn hàng " + order.getId() + " đã được chuyển sang trạng thái HOÀN TẤT.");
        }
    }
}