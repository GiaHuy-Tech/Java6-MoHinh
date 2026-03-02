package com.example.demo.controllers;

import java.math.BigDecimal; 
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

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

    // Hàm tính phí ship dựa trên địa chỉ và tổng tiền
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

    // Hàm bỏ dấu tiếng Việt để so sánh địa chỉ
    public static String unAccent(String s) {
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(temp)
                .replaceAll("")
                .replaceAll("đ", "d");
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("ordersList", ordersRepo.findAll());
        return "admin/orders-mana";
    }

    @PostMapping("/updateStatus")
    public String updateStatus(@RequestParam("id") Integer id,
                               @RequestParam("status") int status) {

        // 1. Tìm đơn hàng
        Orders order = ordersRepo.findById(id).orElse(null);
        if (order == null) return "redirect:/orders-mana";

        // 2. Cập nhật trạng thái đơn hàng (0: Chờ, 1: Xác nhận, 2: Giao, 3: Hoàn tất, 4: Hủy)
        order.setStatus(status);

        // 3. LOGIC TỰ ĐỘNG CẬP NHẬT THANH TOÁN
        // Nếu là VNPAY HOẶC trạng thái là Hoàn tất (3) thì mặc định là đã thanh toán
        if (status == 3 || "VNPAY".equalsIgnoreCase(order.getPaymentMethod())) {
            order.setPaymentStatus(true);
        }

        // 4. TÍNH TOÁN LẠI TỔNG TIỀN (SUBTOTAL + FEESHIP)
        BigDecimal subTotal = order.getOrderDetails()
                .stream()
                .map(d -> d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String fullAddress = (order.getAddress() != null) ? order.getAddress().getFullAddress() : null;
        int feeInt = calculateShippingFee(fullAddress, subTotal.intValue());
        BigDecimal fee = BigDecimal.valueOf(feeInt);

        order.setFeeship(fee);
        order.setTotal(subTotal.add(fee));

        // 5. LƯU VÀO CSDL
        ordersRepo.save(order);

        // 6. GỬI MAIL THÔNG BÁO CHO KHÁCH HÀNG
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

            mailService.sendStatusMail(
                    acc.getEmail(),
                    "Cập nhật đơn hàng #" + order.getId(),
                    "Đơn hàng của bạn hiện tại có trạng thái: " + statusText + 
                    ". Cảm ơn bạn đã mua sắm!"
            );
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