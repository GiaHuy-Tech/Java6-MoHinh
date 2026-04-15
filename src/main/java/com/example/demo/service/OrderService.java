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
    // 1. TẠO ĐƠN HÀNG
    // =====================================================
    @Transactional
    public void createOrder(Account acc, String voucherCode) {

        List<CartDetail> cartList = cartDetailRepo.findByAccount_Id(acc.getId());

        if (cartList == null || cartList.isEmpty()) {
            return;
        }

        BigDecimal rawTotal = BigDecimal.ZERO;

        // ===== CHECK TỒN KHO =====
        for (CartDetail item : cartList) {

            Products product = productRepo.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            Integer buyQty = item.getQuantity();

            if (product.getQuantity() < buyQty) {
                throw new RuntimeException(
                        "Sản phẩm " + product.getName() + " không đủ số lượng trong kho");
            }

            BigDecimal price = product.getPrice();
            rawTotal = rawTotal.add(price.multiply(BigDecimal.valueOf(buyQty)));
        }

        // ===== DISCOUNT =====
        BigDecimal discount = BigDecimal.ZERO;
        if (voucherCode != null && voucherCode.equalsIgnoreCase("SALE10")) {
            discount = rawTotal.multiply(new BigDecimal("0.1"));
        }

        // ===== SHIPPING =====
        BigDecimal feeShip = new BigDecimal("30000");
        if (rawTotal.compareTo(new BigDecimal("1000000")) > 0) {
            feeShip = BigDecimal.ZERO;
        }

        BigDecimal finalTotal = rawTotal.subtract(discount).add(feeShip);

        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        // ===== CREATE ORDER =====
        Orders order = new Orders();
        order.setAccount(acc);
        order.setCreatedDate(new Date());
        order.setTotal(finalTotal);
        order.setStatus(0);
        order.setFeeship(feeShip);
        order.setMoneyDiscounted(discount);
        order.setPaymentStatus(false);

        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            order.setVoucherCode(voucherCode);
        }

        orderRepo.save(order);

        // ===== CREATE ORDER DETAILS + TRỪ KHO =====
        for (CartDetail cd : cartList) {

            Products product = productRepo.findById(cd.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            Integer buyQty = cd.getQuantity();

            // Trừ kho
            int newQty = product.getQuantity() - buyQty;
            product.setQuantity(newQty);

            // Tăng sold
            Integer sold = product.getSold() == null ? 0 : product.getSold();
            product.setSold(sold + buyQty);

            // Nếu hết hàng
            if (newQty <= 0) {
                product.setAvailable(false);
            }

            productRepo.saveAndFlush(product);

            // Tạo order detail
            OrderDetail od = new OrderDetail();
            od.setOrder(order);
            od.setProduct(product);
            od.setQuantity(buyQty);
            od.setPrice(product.getPrice());

            orderDetailRepo.save(od);
        }

        // Xóa cart
        cartDetailRepo.deleteAll(cartList);
    }

    // =====================================================
    // 2. HOÀN TẤT ĐƠN
    // =====================================================
    @Transactional
    public void completeOrder(Orders order) {

        if (order.getStatus() < 4) {
            order.setStatus(4);
            order.setPaymentStatus(true);
            orderRepo.save(order);
        }
    }

    // =====================================================
    // 3. HỦY ĐƠN -> HOÀN KHO
    // =====================================================
    @Transactional
    public void cancelOrder(Orders order) {

        // Load lại order mới nhất từ DB
        Orders dbOrder = orderRepo.findById(order.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Chỉ hoàn kho nếu đơn chưa hoàn tất và chưa hủy
        if (dbOrder.getStatus() == 0 || dbOrder.getStatus() == 1) {

            List<OrderDetail> details = dbOrder.getOrderDetails();

            if (details != null && !details.isEmpty()) {

                for (OrderDetail detail : details) {

                    Products product = productRepo.findById(
                            detail.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

                    Integer qtyReturn = detail.getQuantity();

                    // ===== HOÀN LẠI KHO =====
                    product.setQuantity(product.getQuantity() + qtyReturn);

                    // ===== GIẢM SOLD =====
                    Integer sold = product.getSold() == null ? 0 : product.getSold();
                    product.setSold(Math.max(0, sold - qtyReturn));

                    // ===== CÓ HÀNG LẠI =====
                    if (product.getQuantity() > 0) {
                        product.setAvailable(true);
                    }

                    productRepo.saveAndFlush(product);
                }
            }

            // ===== UPDATE ORDER STATUS =====
            dbOrder.setStatus(5); // 5 = Đã hủy
            orderRepo.saveAndFlush(dbOrder);

            System.out.println("Đơn hàng " + dbOrder.getId() + " đã hủy và hoàn kho.");
        }
    }
}