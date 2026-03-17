package com.example.demo.controllers;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
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

    private static final List<String> SOUTH = Arrays.asList("ho chi minh", "can tho", "long an");

    // Hàm tính phí ship
    private int calculateShippingFee(String address, int subTotal) {
        if (subTotal >= 1000000) return 0;
        if (address == null) return 50000;
        String a = unAccent(address.toLowerCase());
        if (a.contains("can tho")) return 20000;
        for (String s : SOUTH) {
            if (a.contains(s)) return 30000;
        }
        return 50000;
    }

    public static String unAccent(String s) {
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(temp).replaceAll("").replaceAll("đ", "d");
    }

    // --- CẬP NHẬT HÀM LIST VỚI BỘ LỌC ---
    @GetMapping
    public String list(Model model, 
                       @RequestParam(name = "keywords", required = false) String keywords,
                       @RequestParam(name = "status", required = false) Integer status) {
        
        List<Orders> list = ordersRepo.findAll();

        // Lọc theo từ khóa (Tên khách hàng hoặc ID đơn hàng)
        if (keywords != null && !keywords.trim().isEmpty()) {
            list = list.stream()
                .filter(o -> (o.getAccount() != null && o.getAccount().getFullName().toLowerCase().contains(keywords.toLowerCase())) 
                          || String.valueOf(o.getId()).contains(keywords))
                .collect(Collectors.toList());
        }

        // Lọc theo trạng thái
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

        order.setStatus(status);

        // Logic thanh toán tự động
        if (status == 3 || "VNPAY".equalsIgnoreCase(order.getPaymentMethod())) {
            order.setPaymentStatus(true);
        }

        // Tính lại tiền
        BigDecimal subTotal = order.getOrderDetails().stream()
                .map(d -> d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String fullAddress = (order.getAddress() != null) ? order.getAddress().getFullAddress() : null;
        BigDecimal fee = BigDecimal.valueOf(calculateShippingFee(fullAddress, subTotal.intValue()));

        order.setFeeship(fee);
        order.setTotal(subTotal.add(fee));
        ordersRepo.save(order);

        // Gửi mail
        Account acc = order.getAccount();
        if (acc != null && acc.getEmail() != null) {
            String statusText = switch (status) {
                case 0 -> "Chờ xác nhận";
                case 1 -> "Đã xác nhận";
                case 2 -> "Đang giao hàng";
                case 3 -> "Đã hoàn tất (Thành công)";
                case 4 -> "Đã hủy";
                default -> "Không xác định";
            };
            mailService.sendStatusMail(acc.getEmail(), "Cập nhật đơn hàng #" + order.getId(), 
                "Đơn hàng của bạn hiện tại có trạng thái: " + statusText + ". Cảm ơn bạn!");
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