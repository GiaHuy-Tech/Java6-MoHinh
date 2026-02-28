package com.example.demo.controllers;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Account;
import com.example.demo.model.Orders;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.OrdersRepository;
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

    private static final List<String> SOUTH = Arrays.asList("ho chi minh","can tho","long an");

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

        Orders order = ordersRepo.findById(id).orElse(null);

        if (order != null) {

            order.setStatus(status);

            double subTotal = order.getOrderDetails()
                    .stream()
                    .mapToDouble(d -> d.getPrice() * d.getQuantity())
                    .sum();

            int fee = calculateShippingFee(
                    order.getAddress().getFullAddress(),
                    (int) subTotal
            );

            order.setFeeship((double) fee);
            order.setTotal(subTotal + fee);

            ordersRepo.save(order);

            Account acc = order.getAccount();

            if (acc != null && acc.getEmail() != null) {
                mailService.sendStatusMail(
                        acc.getEmail(),
                        "Cập nhật đơn #" + order.getId(),
                        "Trạng thái mới: " + status
                );
            }
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