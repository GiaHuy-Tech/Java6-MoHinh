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
    private CartDetailRepository cartDetailRepo;

    @Autowired
    private OrdersRepository orderRepo;

    @Autowired
    private OrdersDetailRepository orderDetailRepo;

    @Autowired
    private ProductRepository productRepo;

    // =====================================================
    // 1. TẠO ĐƠN HÀNG - CHECKOUT
    // =====================================================
    @Transactional
    public void createOrder(Account acc, String voucherCode) {

        List<CartDetail> cartList = cartDetailRepo.findByAccount_Id(acc.getId());

        if (cartList == null || cartList.isEmpty()) {
            return;
        }

        BigDecimal rawTotal = BigDecimal.ZERO;

        // ===============================
        // CHECK TỒN KHO + TÍNH TỔNG TIỀN
        // ===============================
        for (CartDetail item : cartList) {

            Products product = productRepo.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            Integer buyQty = item.getQuantity();

            // Kiểm tra tồn kho
            if (product.getQuantity() < buyQty) {
                throw new RuntimeException(
                        "Sản phẩm " + product.getName() + " không đủ số lượng trong kho");
            }

            BigDecimal price = product.getPrice();
            rawTotal = rawTotal.add(price.multiply(BigDecimal.valueOf(buyQty)));
        }

        // ===============================
        // TÍNH GIẢM GIÁ
        // ===============================
        BigDecimal discount = BigDecimal.ZERO;
        if (voucherCode != null && voucherCode.equalsIgnoreCase("SALE10")) {
            discount = rawTotal.multiply(new BigDecimal("0.1"));
        }

        // ===============================
        // TÍNH PHÍ SHIP
        // ===============================
        BigDecimal feeShip = new BigDecimal("30000");
        if (rawTotal.compareTo(new BigDecimal("1000000")) > 0) {
            feeShip = BigDecimal.ZERO;
        }

        // ===============================
        // TỔNG THANH TOÁN
        // ===============================
        BigDecimal finalTotal = rawTotal.subtract(discount).add(feeShip);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        // ===============================
        // TẠO ORDER
        // ===============================
        Orders order = new Orders();
        order.setAccount(acc);
        order.setCreatedDate(new Date());
        order.setTotal(finalTotal);
        order.setStatus(0); // Chờ xác nhận
        order.setFeeship(feeShip);
        order.setMoneyDiscounted(discount);
        order.setPaymentStatus(false);

        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            order.setVoucherCode(voucherCode);
        }

        orderRepo.save(order);

        // ===============================
        // TẠO ORDER DETAIL + TRỪ KHO
        // ===============================
        for (CartDetail cd : cartList) {

            Products product = productRepo.findById(cd.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            Integer buyQty = cd.getQuantity();

            // Trừ số lượng kho
            product.setQuantity(product.getQuantity() - buyQty);

            // Tăng sold
            Integer sold = product.getSold() == null ? 0 : product.getSold();
            product.setSold(sold + buyQty);

            // Nếu hết hàng -> unavailable
            if (product.getQuantity() <= 0) {
                product.setAvailable(false);
            }

            productRepo.save(product);

            // Lưu chi tiết đơn hàng
            OrderDetail od = new OrderDetail();
            od.setOrder(order);
            od.setProduct(product);
            od.setQuantity(buyQty);
            od.setPrice(product.getPrice());

            orderDetailRepo.save(od);
        }

        // ===============================
        // XÓA GIỎ HÀNG
        // ===============================
        cartDetailRepo.deleteAll(cartList);
    }

    // =====================================================
    // 2. HOÀN TẤT ĐƠN HÀNG
    // =====================================================
    @Transactional
    public void completeOrder(Orders order) {

        // Chỉ cập nhật trạng thái, KHÔNG trừ kho nữa
        if (order.getStatus() < 4) {
            order.setStatus(4); // Hoàn tất
            order.setPaymentStatus(true);
            orderRepo.save(order);

            System.out.println("Đơn hàng " + order.getId() + " đã hoàn tất.");
        }
    }

    // =====================================================
    // 3. HỦY ĐƠN HÀNG - HOÀN KHO
    // =====================================================
    @Transactional
    public void cancelOrder(Orders order) {

        // Chỉ hoàn kho nếu đơn chưa hoàn tất và chưa hủy
        if (order.getStatus() == 0 || order.getStatus() == 1) {

            List<OrderDetail> details = order.getOrderDetails();

            if (details != null) {
                for (OrderDetail detail : details) {

                    Products product = detail.getProduct();

                    if (product != null) {

                        // Hoàn lại kho
                        product.setQuantity(product.getQuantity() + detail.getQuantity());

                        // Giảm sold
                        Integer sold = product.getSold() == null ? 0 : product.getSold();
                        product.setSold(Math.max(0, sold - detail.getQuantity()));

                        // Có hàng lại -> available
                        if (product.getQuantity() > 0) {
                            product.setAvailable(true);
                        }

                        productRepo.save(product);
                    }
                }
            }

            order.setStatus(5); // Đã hủy
            orderRepo.save(order);

            System.out.println("Đơn hàng " + order.getId() + " đã bị hủy và hoàn kho.");
        }
    }
}