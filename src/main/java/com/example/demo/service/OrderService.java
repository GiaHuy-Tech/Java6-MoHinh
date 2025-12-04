package com.example.demo.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Orders;
import com.example.demo.repository.OrdersRepository;

@Service
public class OrderService {

    @Autowired
    private OrdersRepository orderRepository;

    // ✅ Lấy toàn bộ đơn hàng (không phân trang)
    public List<Orders> getAllOrders() {
        return orderRepository.findAll();
    }

    // ✅ Lấy đơn hàng có phân trang
    public Page<Orders> getPagedOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    // ✅ Đếm tổng số đơn hàng
    public long countOrders() {
        return orderRepository.count();
    }

    // ✅ Tìm đơn hàng theo ID
    public Orders getOrderById(Integer id) {
        Optional<Orders> order = orderRepository.findById(id);
        return order.orElse(null);
    }

    // ✅ Thêm mới đơn hàng
    @Transactional
    public Orders createOrder(Orders order) {
        if (order.getCreatedDate() == null) {
            order.setCreatedDate(new Date());
        }
        order.setStatus(0); // mặc định là "chờ xử lý"
        return orderRepository.save(order);
    }

    // ✅ Cập nhật đơn hàng
    @Transactional
    public Orders updateOrder(Integer id, Orders updatedOrder) {
        Orders existing = orderRepository.findById(id).orElse(null);
        if (existing == null) return null;

        existing.setAddress(updatedOrder.getAddress());
        existing.setTotal(updatedOrder.getTotal());
        existing.setStatus(updatedOrder.getStatus());
        existing.setFeeship(updatedOrder.getFeeship());
        existing.setPaymentMethod(updatedOrder.getPaymentMethod());
        existing.setPaymentStatus(updatedOrder.getPaymentStatus());
        existing.setPhone(updatedOrder.getPhone());
        existing.setAccountId(updatedOrder.getAccountId());

        return orderRepository.save(existing);
    }

    // ✅ Xóa đơn hàng
    @Transactional
    public boolean deleteOrder(Integer id) {
        if (!orderRepository.existsById(id)) return false;
        orderRepository.deleteById(id);
        return true;
    }

    // ✅ Cập nhật trạng thái đơn hàng (VD: từ “chờ xử lý” → “đã xác nhận”)
    @Transactional
    public boolean updateStatus(Integer id, int newStatus) {
        Orders order = orderRepository.findById(id).orElse(null);
        if (order == null) return false;
        order.setStatus(newStatus);
        orderRepository.save(order);
        return true;
    }

    // ✅ Lấy các đơn hàng gần nhất (VD: 5 đơn mới nhất)
    public List<Orders> getRecentOrders() {
        return orderRepository.findTop5ByOrderByCreatedDateDesc();
    }

    // ✅ Thống kê tổng doanh thu
    public long getTotalRevenue() {
        List<Orders> all = orderRepository.findAll();
        return all.stream()
                  .filter(o -> o.getStatus() == 3) // chỉ tính đơn hoàn tất
                  .mapToLong(Orders::getTotal)
                  .sum();
    }
}
