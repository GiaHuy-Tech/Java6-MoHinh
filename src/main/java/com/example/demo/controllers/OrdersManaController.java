package com.example.demo.controllers;

import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Account;
import com.example.demo.model.Orders;
import com.example.demo.model.OrderDetail;
import com.example.demo.repository.OrdersRepository;
import com.example.demo.repository.AccountRepository;
import com.example.demo.service.MailService;

@Controller
@RequestMapping("/orders-mana")
public class OrdersManaController {

    @Autowired
    private OrdersRepository ordersRepo;

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private MailService mailService;

    // ✅ Danh sách đơn hàng
    @GetMapping
    public String list(Model model) {
        model.addAttribute("ordersList", ordersRepo.findAll());
        model.addAttribute("order", new Orders());
        model.addAttribute("accounts", accountRepo.findAll());
        return "admin/orders-mana";
    }

    // ✅ Thêm mới đơn hàng
    @PostMapping("/add")
    public String add(
            @RequestParam("accountId") Integer accountId,
            @ModelAttribute("order") Orders order) {

        Account acc = accountRepo.findById(accountId).orElse(null);
        order.setAccountId(acc);
        order.setCreatedDate(new Date());
        ordersRepo.save(order);
        return "redirect:/orders-mana";
    }

    // ✅ Form sửa đơn hàng (chỉ đổi trạng thái)
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Integer id, Model model) {
        Orders order = ordersRepo.findById(id).orElse(null);
        if (order == null) {
            return "redirect:/orders-mana";
        }
        model.addAttribute("order", order);
        model.addAttribute("ordersList", ordersRepo.findAll());
        model.addAttribute("accounts", accountRepo.findAll());
        return "admin/order-edit"; // Trang riêng để chỉnh trạng thái
    }

    // ✅ Cập nhật trạng thái đơn hàng và gửi mail
    @PostMapping("/updateStatus")
    public String updateStatus(@RequestParam("id") Integer id,
                               @RequestParam("status") int status) {

        Orders order = ordersRepo.findById(id).orElse(null);
        if (order != null) {
            order.setStatus(status);
            ordersRepo.save(order);

            Account acc = order.getAccountId();
            if (acc != null && acc.getEmail() != null) {
                String subject = "Cập nhật trạng thái đơn hàng #" + order.getId();
                String body = "Xin chào " + acc.getFullName() + ",\n\n"
                        + "Trạng thái đơn hàng của bạn vừa được cập nhật: "
                        + getStatusText(status)
                        + "\n\nCảm ơn bạn đã mua hàng tại Mom Physic High End Model!";
                mailService.sendStatusMail(acc.getEmail(), subject, body);
            }
        }
        return "redirect:/orders-mana";
    }

    private String getStatusText(int status) {
        switch (status) {
            case 0: return "Chờ xử lý";
            case 1: return "Đã xác nhận";
            case 2: return "Đang giao hàng";
            case 3: return "Hoàn tất";
            case 4: return "Đã hủy";
            default: return "Không xác định";
        }
    }

    // ✅ Hiển thị chi tiết giỏ hàng của một đơn hàng
    @GetMapping("/cart/{orderId}")
    public String cartDetail(@PathVariable("orderId") Integer orderId, Model model) {
        Orders order = ordersRepo.findById(orderId).orElse(null);
        if (order == null) {
            return "redirect:/orders-mana"; // nếu không có đơn hàng
        }

        // Gửi danh sách chi tiết đơn hàng sang view
        model.addAttribute("order", order);
        model.addAttribute("orderDetails", order.getOrderDetails()); // List<OrderDetail>

        return "admin/order-detail"; // Trang hiển thị chi tiết giỏ hàng
    }
    @GetMapping("/detail/{id}")
    public String orderDetail(@PathVariable("id") Integer id, Model model) {
        Orders order = ordersRepo.findById(id).orElse(null);
        if (order == null) {
            return "redirect:/orders-mana"; // nếu không có đơn hàng
        }

        model.addAttribute("order", order);
        model.addAttribute("orderDetails", order.getOrderDetails()); // List<OrderDetail>
        return "admin/order-detail"; // trang Thymeleaf mới
    }
}
