package com.example.demo.controllers;

import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.demo.model.Account;
import com.example.demo.model.Orders;
import com.example.demo.repository.OrdersRepository;
import com.example.demo.service.MailService;
import com.example.demo.repository.AccountRepository;

@Controller
@RequestMapping("/orders-mana")
public class OrdersManaController {

    @Autowired
    OrdersRepository ordersRepo;

    @Autowired
    AccountRepository accountRepo;
    @Autowired
    MailService mailService;


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

    // ✅ Form sửa chỉ để đổi trạng thái
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Integer id, Model model) {
        Orders order = ordersRepo.findById(id).orElse(null);
        if (order == null) {
            return "redirect:/orders-mana";
        }
        model.addAttribute("order", order);
        model.addAttribute("ordersList", ordersRepo.findAll());
        model.addAttribute("accounts", accountRepo.findAll());
        return "admin/order-edit"; // 👉 trang riêng chỉ để chỉnh trạng thái
    }


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
    
}
