package com.example.demo.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Account;
import com.example.demo.model.Orders;
import com.example.demo.repository.OrdersRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class OrderController {

    @Autowired
    private HttpSession session;

    @Autowired
    private OrdersRepository orderRepo;

    /**
     * Hiển thị trang "Đơn hàng của tôi" có chức năng Sắp xếp
     */
    @GetMapping("/orders")
    public String viewOrders(Model model,
                             @RequestParam(name = "sort", defaultValue = "newest") String sort) {

        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        List<Orders> orders;

        // Xử lý logic sắp xếp dựa trên tham số 'sort' từ URL
        if ("oldest".equals(sort)) {
            // Cũ nhất -> Mới nhất (Ascending)
            // ⚠️ Lưu ý: Bạn cần khai báo hàm này trong OrdersRepository (xem bên dưới)
            orders = orderRepo.findByAccountId_IdOrderByCreatedDateAsc(account.getId());
        } else {
            // Mới nhất -> Cũ nhất (Descending) - Mặc định
            orders = orderRepo.findByAccountId_IdOrderByCreatedDateDesc(account.getId());
        }

        model.addAttribute("orders", orders);
        model.addAttribute("sort", sort); // Gửi lại biến sort để giữ trạng thái select box

        return "client/orders";
    }

    /**
     * Xem chi tiết đơn hàng
     */
    @GetMapping("/orders/detail/{id}")
    public String viewOrderDetail(@PathVariable("id") Integer orderId, Model model) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) {
			return "redirect:/login";
		}

        Optional<Orders> orderOpt = orderRepo.findById(orderId);

        if (orderOpt.isPresent()) {
            Orders order = orderOpt.get();
            // Bảo mật: Chỉ xem được đơn của chính mình
            if (order.getAccountId().getId().equals(account.getId())) {
                model.addAttribute("order", order);
                model.addAttribute("orderDetails", order.getOrderDetails());
                return "client/order-detail";
            }
        }

        return "redirect:/orders";
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
            // Chỉ cho phép hủy đơn của chính user và trạng thái 0 (Chờ xử lý) hoặc 1 (Đã xác nhận)
            if (order.getAccountId().getId().equals(account.getId()) &&
                (order.getStatus() == 0 || order.getStatus() == 1)) {

                order.setStatus(4); // 4 = Đã hủy
                orderRepo.save(order);
            }
        }

        return "redirect:/orders";
    }
}