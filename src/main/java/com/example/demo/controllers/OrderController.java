package com.example.demo.controllers;

import com.example.demo.model.Account;
import com.example.demo.model.Orders;
import com.example.demo.repository.OrdersRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
public class OrderController {

    @Autowired
    private HttpSession session;

    @Autowired
    private OrdersRepository orderRepo;

    /**
     * Hiển thị trang "Đơn hàng của tôi"
     */
    @GetMapping("/orders")
    public String viewOrders(Model model) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        List<Orders> orders = orderRepo.findByAccountId_IdOrderByCreatedDateDesc(account.getId());
        model.addAttribute("orders", orders);

        return "client/orders"; // Trỏ đến file orders.html
    }

    /**
     * Xử lý hủy đơn
     */
    @PostMapping("/orders/cancel/{id}")
    public String cancelOrder(@PathVariable("id") Integer orderId) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        Optional<Orders> optionalOrder = orderRepo.findById(orderId);
        if (optionalOrder.isPresent()) {
            Orders order = optionalOrder.get();
            // Chỉ cho phép hủy đơn của chính user và trạng thái 0 hoặc 1
            if (order.getAccountId().getId().equals(account.getId()) &&
                (order.getStatus() == 0 || order.getStatus() == 1)) {

                order.setStatus(4); // 4 = Đã hủy
                orderRepo.save(order);
            }
        }

        return "redirect:/orders";
    }
}
