package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.OrderDetail;
import com.example.demo.model.Orders;
import com.example.demo.model.Products;
import com.example.demo.repository.OrdersRepository;
import com.example.demo.repository.ProductRepository;

@Service
public class OrderTaskService {

    @Autowired
    private OrdersRepository orderRepo;

    @Autowired
    private ProductRepository productRepo;

    // Chạy mỗi giờ một lần để kiểm tra (Cron: giây phút giờ ngày tháng thứ)
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void autoConfirmOrders() {
        // 1. Lấy thời điểm cách đây 2 ngày (Đã sửa thành LocalDateTime để khớp với Repository)
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);

        // 2. Tìm các đơn hàng có Status = 3 (Đã giao hàng) và ngày tạo <= cách đây 2 ngày
        List<Orders> overdueOrders = orderRepo.findByStatusAndCreatedDateBefore(3, twoDaysAgo);

        for (Orders order : overdueOrders) {
            System.out.println("Tự động hoàn tất đơn hàng ID: " + order.getId());

            // Xử lý logic giống hệt lúc bấm xác nhận
            // 1. Trừ kho
            List<OrderDetail> details = order.getOrderDetails();
            if (details != null) {
                for (OrderDetail detail : details) {
                    Products product = detail.getProduct();
                    if (product != null) {
                        int newQty = Math.max(0, product.getQuantity() - detail.getQuantity());
                        product.setQuantity(newQty);
                        if (newQty <= 0) {
							product.setAvailable(false);
						}
                        productRepo.save(product);
                    }
                }
            }

            // 2. Cập nhật trạng thái
            order.setStatus(4); // Hoàn tất
            order.setPaymentStatus(true);
            orderRepo.save(order);
        }
    } // Đóng hàm autoConfirmOrders

} // Đóng class OrderTaskService