package com.example.demo.controllers;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.demo.model.Account;
import com.example.demo.model.Orders;
import com.example.demo.repository.OrdersRepository;
import com.example.demo.service.MailService;

@Controller
@RequestMapping("/orders-mana")
public class OrdersManaController {

    @Autowired
    private OrdersRepository ordersRepo;

    @Autowired
    private MailService mailService;

    @GetMapping
    public String list(Model model, 
                       @RequestParam(name = "keywords", required = false) String keywords,
                       @RequestParam(name = "status", required = false) Integer status) {
        
        List<Orders> list = ordersRepo.findAll();

        if (keywords != null && !keywords.trim().isEmpty()) {
            list = list.stream()
                .filter(o -> (o.getAccount() != null && o.getAccount().getFullName().toLowerCase().contains(keywords.toLowerCase())) 
                          || String.valueOf(o.getId()).contains(keywords))
                .collect(Collectors.toList());
        }

        if (status != null) {
            list = list.stream()
                .filter(o -> o.getStatus() == status)
                .collect(Collectors.toList());
        }

        // Sắp xếp đơn mới nhất lên đầu
        list.sort((o1, o2) -> o2.getId().compareTo(o1.getId()));

        model.addAttribute("ordersList", list);
        model.addAttribute("keywords", keywords);
        model.addAttribute("selectedStatus", status);
        
        return "admin/orders-mana";
    }

    @PostMapping("/updateStatus")
    public String updateStatus(@RequestParam("id") Integer id, @RequestParam("status") int status) {
        Orders order = ordersRepo.findById(id).orElse(null);
        if (order == null) return "redirect:/orders-mana";

        // 1. Admin KHÔNG được phép chỉnh trạng thái sang 4 (Hoàn tất)
        if (status == 4) return "redirect:/orders-mana";

        // 2. Nếu đơn đã Hoàn tất (4) hoặc Đã hủy (5), không cho phép đổi nữa
        if (order.getStatus() == 4 || order.getStatus() == 5) {
            return "redirect:/orders-mana";
        }

        order.setStatus(status);

        // 3. Tự động cập nhật Payment Status nếu là VNPAY hoặc khi Admin xác nhận Đã giao xong (3)
        if ("VNPAY".equalsIgnoreCase(order.getPaymentMethod()) || status == 3) {
            order.setPaymentStatus(true);
        }

        ordersRepo.save(order);

        // 4. Gửi mail thông báo cho khách (Tùy chọn)
        Account acc = order.getAccount();
        if (acc != null && acc.getEmail() != null) {
            String statusText = switch (status) {
                case 1 -> "Đã xác nhận";
                case 2 -> "Đang giao hàng";
                case 3 -> "Đã giao đến nơi (Vui lòng xác nhận trên web)";
                case 5 -> "Đã hủy";
                default -> "Cập nhật";
            };
            mailService.sendStatusMail(acc.getEmail(), "Cập nhật đơn hàng #" + order.getId(), 
                "Đơn hàng của bạn đã chuyển sang trạng thái: " + statusText);
        }

        return "redirect:/orders-mana";
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        Orders order = ordersRepo.findById(id).orElse(null);
        if (order == null) return "redirect:/orders-mana";
        model.addAttribute("order", order);
        model.addAttribute("orderDetails", order.getOrderDetails());
        return "admin/order-detail";
    }
}