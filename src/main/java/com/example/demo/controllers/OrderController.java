package com.example.demo.controllers;

import com.example.demo.model.Account;
import com.example.demo.model.Orders;
import com.example.demo.repository.OrdersRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

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
}

