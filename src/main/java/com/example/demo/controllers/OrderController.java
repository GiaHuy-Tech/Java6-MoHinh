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
import com.example.demo.service.OrderService; // Import Service mới
import jakarta.servlet.http.HttpSession;

@Controller
public class OrderController {

    @Autowired
    private HttpSession session;

    @Autowired
    private OrdersRepository orderRepo;
    
    @Autowired
    private OrderService orderService; // Inject OrderService vào đây

    @GetMapping("/orders")
    public String viewOrders(Model model, @RequestParam(name = "sort", defaultValue = "newest") String sort) {
        Account account = getAccount();
        if (account == null) return "redirect:/login";

        List<Orders> orders;
        if ("oldest".equals(sort)) {
            orders = orderRepo.findByAccount_IdOrderByCreatedDateAsc(account.getId());
        } else {
            orders = orderRepo.findByAccount_IdOrderByCreatedDateDesc(account.getId());
        }

        model.addAttribute("orders", orders);
        model.addAttribute("sort", sort);
        model.addAttribute("user", account);
        return "client/orders";
    }

    @GetMapping("/orders/detail/{id}")
    public String viewOrderDetail(@PathVariable("id") Integer orderId, Model model) {
        Account account = getAccount();
        if (account == null) return "redirect:/login";

        Optional<Orders> orderOpt = orderRepo.findById(orderId);
        if (orderOpt.isPresent()) {
            Orders order = orderOpt.get();
            if (order.getAccount().getId().equals(account.getId())) {
                model.addAttribute("order", order);
                model.addAttribute("orderDetails", order.getOrderDetails());
                model.addAttribute("user", account);
                return "client/order-detail";
            }
        }
        return "redirect:/orders";
    }

    // =====================================================
    // XÁC NHẬN ĐƠN HÀNG (ĐÃ RÚT GỌN)
    // =====================================================
    @PostMapping("/orders/confirm/{id}")
    public String confirmOrder(@PathVariable("id") Integer orderId) {
        Account account = getAccount();
        if (account == null) return "redirect:/login";

        Optional<Orders> optionalOrder = orderRepo.findById(orderId);
        if (optionalOrder.isPresent()) {
            Orders order = optionalOrder.get();

            // Kiểm tra đúng chủ đơn và trạng thái là Đã giao (3)
            if (order.getAccount().getId().equals(account.getId()) && order.getStatus() == 3) {
                
                // GỌI DUY NHẤT HÀM NÀY: Nó tự trừ kho và tự đổi status
                orderService.completeOrder(order);
                
            }
        }
        return "redirect:/orders";
    }

    @PostMapping("/orders/cancel/{id}")
    public String cancelOrder(@PathVariable("id") Integer orderId) {
        Account account = getAccount();
        if (account == null) return "redirect:/login";

        Optional<Orders> optionalOrder = orderRepo.findById(orderId);
        if (optionalOrder.isPresent()) {
            Orders order = optionalOrder.get();
            if (order.getAccount().getId().equals(account.getId()) && (order.getStatus() == 0 || order.getStatus() == 1)) {
                order.setStatus(5); // 5 = Đã hủy
                orderRepo.save(order);
            }
        }
        return "redirect:/orders";
    }

    // Hàm phụ để lấy account cho gọn
    private Account getAccount() {
        Account acc = (Account) session.getAttribute("account");
        return (acc != null) ? acc : (Account) session.getAttribute("user");
    }
}