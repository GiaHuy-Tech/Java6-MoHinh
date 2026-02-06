package com.example.demo.controllers;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.MembershipService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired private HttpSession session;
    @Autowired private OrdersRepository orderRepo;
    @Autowired private CartDetailRepository cartRepo;
    @Autowired private AccountRepository accountRepo;
    @Autowired private MembershipService membershipService;

    // ===== VNPAY SANDBOX CONFIG =====
    private static final String VNP_TMN_CODE = "XXXXXXX";        // TMN CODE
    private static final String VNP_HASH_SECRET = "YYYYYYYY";   // SECRET KEY
    private static final String VNP_PAY_URL =
            "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    private static final String VNP_RETURN_URL =
    	    "https://punchy-sondra-unanswerably.ngrok-free.dev/checkout/vnpay-return";


    // ===== CHECKOUT PAGE =====
    @GetMapping
    public String checkoutPage(Model model) {

        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        List<CartDetail> cart = cartRepo.findByCart_Account_Id(account.getId());
        if (cart.isEmpty()) return "redirect:/cart";

        int subTotal = cart.stream()
                .mapToInt(i -> i.getPrice() * i.getQuantity())
                .sum();

        int shippingFee = subTotal >= 1_000_000 ? 0 : 30000;
        int finalTotal = subTotal + shippingFee;

        model.addAttribute("account", account);
        model.addAttribute("cartDetails", cart);
        model.addAttribute("total", subTotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("finalTotal", finalTotal);

        return "client/checkout";
    }

    // ===== CHECKOUT SUBMIT =====
    @PostMapping
    public String checkout(@RequestParam("paymentMethod") String paymentMethod,
                           HttpServletRequest request) {

        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        List<CartDetail> cart = cartRepo.findByCart_Account_Id(account.getId());
        if (cart.isEmpty()) return "redirect:/cart";

        int subTotal = cart.stream()
                .mapToInt(i -> i.getPrice() * i.getQuantity())
                .sum();

        int shippingFee = subTotal >= 1_000_000 ? 0 : 30000;
        int finalTotal = subTotal + shippingFee;

        Orders order = new Orders();
        order.setAccountId(account);
        order.setCreatedDate(new Date());
        order.setTotal(finalTotal);                 // ✅ TỔNG CUỐI
        order.setPaymentStatus(false);
        order.setPaymentMethod(paymentMethod);
        order.setNote("DH" + System.currentTimeMillis()); // <= 34 ký tự

        Orders savedOrder = orderRepo.save(order);

        if ("VNPAY".equals(paymentMethod)) {
            return "redirect:" + createVNPayUrl(savedOrder, request);
        }

        return "redirect:/orders";
    }

    // ===== CREATE VNPAY URL =====
    private String createVNPayUrl(Orders order, HttpServletRequest request) {

        try {
            Map<String, String> params = new TreeMap<>();

            params.put("vnp_Version", "2.1.0");
            params.put("vnp_Command", "pay");
            params.put("vnp_TmnCode", VNP_TMN_CODE);
            params.put("vnp_Amount", String.valueOf(order.getTotal() * 100));
            params.put("vnp_CurrCode", "VND");

            params.put("vnp_TxnRef", order.getNote());
            params.put("vnp_OrderInfo", "Thanh toan don hang");
            params.put("vnp_OrderType", "other");

            params.put("vnp_Locale", "vn");
            params.put("vnp_ReturnUrl", VNP_RETURN_URL);
            params.put("vnp_IpAddr", request.getRemoteAddr());

            String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            params.put("vnp_CreateDate", time);

            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            for (Map.Entry<String, String> e : params.entrySet()) {
                hashData.append(e.getKey()).append("=")
                        .append(e.getValue()).append("&");

                query.append(e.getKey()).append("=")
                        .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                        .append("&");
            }

            hashData.deleteCharAt(hashData.length() - 1);
            query.deleteCharAt(query.length() - 1);

            String secureHash = hmacSHA512(VNP_HASH_SECRET, hashData.toString());

            return VNP_PAY_URL + "?" + query + "&vnp_SecureHash=" + secureHash;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ===== VNPAY RETURN (VERIFY HASH) =====
    @GetMapping("/vnpay-return")
    public String vnpayReturn(@RequestParam Map<String, String> params) {

        String vnpSecureHash = params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        StringBuilder hashData = new StringBuilder();
        params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> hashData.append(e.getKey())
                        .append("=")
                        .append(e.getValue())
                        .append("&"));

        hashData.deleteCharAt(hashData.length() - 1);

        String myHash = hmacSHA512(VNP_HASH_SECRET, hashData.toString());

        if (myHash.equalsIgnoreCase(vnpSecureHash)
                && "00".equals(params.get("vnp_ResponseCode"))) {

            String orderCode = params.get("vnp_TxnRef");

            orderRepo.findByNote(orderCode).ifPresent(order -> {
                if (!order.getPaymentStatus()) {
                    order.setPaymentStatus(true);
                    orderRepo.save(order);
                    updateMembership(order.getAccountId(), order.getTotal());
                }
            });
        }

        return "redirect:/orders";
    }

    // ===== MEMBERSHIP =====
    private void updateMembership(Account account, int amount) {
        long total = Optional.ofNullable(account.getTotalSpending()).orElse(0L);
        account.setTotalSpending(total + amount);
        membershipService.updateMembershipLevel(account);
        accountRepo.save(account);
        session.setAttribute("account", account);
    }

    // ===== HASH =====
    private String hmacSHA512(String key, String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));

            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
