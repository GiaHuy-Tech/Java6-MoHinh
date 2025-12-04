package com.example.demo.controllers;

import java.util.Date; 
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.repository.CartDetailRepository;
import com.example.demo.repository.OrdersDetailRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private HttpSession session;

    @Autowired
    private CartDetailRepository cartDetailRepo;

    @Autowired
    private OrdersRepository orderRepo;

    @Autowired
    private OrdersDetailRepository orderDetailRepo;

    /**
     * ✅ GET: Hiển thị trang thanh toán
     */
    @GetMapping
    public String viewCheckout(Model model) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        List<CartDetail> cartDetails = cartDetailRepo.findByCart_Account_Id(account.getId());
        if (cartDetails.isEmpty()) {
            return "redirect:/cart";
        }

        int total = cartDetails.stream()
                .mapToInt(cd -> cd.getPrice() * cd.getQuantity())
                .sum();

        model.addAttribute("cartDetails", cartDetails);
        model.addAttribute("total", total);
        model.addAttribute("account", account);

        return "client/checkout";
    }

    /**
     * ✅ POST: Xử lý đặt hàng
     */
    @PostMapping
    public String processCheckout(
            @RequestParam("address") String address,
            @RequestParam("phone") String phone,
            @RequestParam("paymentMethod") String paymentMethod,
            Model model) {

        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        List<CartDetail> cartDetails = cartDetailRepo.findByCart_Account_Id(account.getId());
        if (cartDetails.isEmpty()) {
            return "redirect:/cart";
        }

        // ✅ Tính tổng tiền
        int total = cartDetails.stream()
                .mapToInt(cd -> cd.getPrice() * cd.getQuantity())
                .sum();

        // ✅ Tạo và lưu Orders
        Orders order = new Orders();
        order.setAccountId(account);
        order.setCreatedDate(new Date());
        order.setAddress(address);
        order.setPhone(phone);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(false); // Chưa thanh toán
        order.setFeeship(30000); // Phí ship cố định
        order.setTotal(total + order.getFeeship());
        order.setStatus(0); // 0: chờ xử lý

        Orders savedOrder = orderRepo.save(order);

        // ✅ Tạo danh sách chi tiết đơn hàng
        List<OrderDetail> orderDetails = cartDetails.stream().map(item -> {
            OrderDetail detail = new OrderDetail();
            detail.setOrders(savedOrder);
            detail.setProductId(item.getProduct());
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getPrice());
            return detail;
        }).toList();

        // ✅ Lưu toàn bộ OrderDetail
        orderDetailRepo.saveAll(orderDetails);

        // ✅ Xóa giỏ hàng sau khi thanh toán
        cartDetailRepo.deleteAll(cartDetails);

        // ✅ Chuyển về trang danh sách đơn hàng
        return "redirect:/orders";
    }
}
