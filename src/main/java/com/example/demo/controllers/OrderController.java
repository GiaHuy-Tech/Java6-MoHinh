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
import com.example.demo.model.OrderDetail;
import com.example.demo.model.Products;
import com.example.demo.repository.OrdersRepository;
import com.example.demo.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class OrderController {

    @Autowired
    private HttpSession session;

    @Autowired
    private OrdersRepository orderRepo;
    
    @Autowired
    private ProductRepository productRepo;

    @GetMapping("/orders")
    public String viewOrders(Model model, @RequestParam(name = "sort", defaultValue = "newest") String sort) {
        Account account = (Account) session.getAttribute("account");
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
        Account account = (Account) session.getAttribute("account");
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

    @PostMapping("/orders/confirm/{id}")
    public String confirmOrder(@PathVariable("id") Integer orderId) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        Optional<Orders> optionalOrder = orderRepo.findById(orderId);
        if (optionalOrder.isPresent()) {
            Orders order = optionalOrder.get();

            // CHỈ XỬ LÝ KHI ADMIN ĐÃ GIAO XONG (STATUS = 3)
            if (order.getAccount().getId().equals(account.getId()) && order.getStatus() == 3) {
                // 1. TRỪ KHO SẢN PHẨM
                List<OrderDetail> details = order.getOrderDetails();
                if (details != null) {
                    for (OrderDetail detail : details) {
                        Products product = detail.getProduct();
                        if (product != null) {
                            int newQty = Math.max(0, product.getQuantity() - detail.getQuantity());
                            product.setQuantity(newQty);
                            if (newQty <= 0) product.setAvailable(false);
                            productRepo.save(product);
                        }
                    }
                }
                // 2. CẬP NHẬT TRẠNG THÁI
                order.setStatus(4); // 4 = Hoàn tất
                order.setPaymentStatus(true);
                orderRepo.save(order);
            }
        }
        return "redirect:/orders";
    }

    @PostMapping("/orders/cancel/{id}")
    public String cancelOrder(@PathVariable("id") Integer orderId) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        Optional<Orders> optionalOrder = orderRepo.findById(orderId);
        if (optionalOrder.isPresent()) {
            Orders order = optionalOrder.get();
            // Khách chỉ hủy được khi status = 0 hoặc 1
            if (order.getAccount().getId().equals(account.getId()) && (order.getStatus() == 0 || order.getStatus() == 1)) {
                order.setStatus(5); // 5 = Đã hủy
                orderRepo.save(order);
            }
        }
        return "redirect:/orders";
    }
}