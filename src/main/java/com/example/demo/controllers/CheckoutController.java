package com.example.demo.controllers;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired private HttpSession session;
    @Autowired private CartDetailRepository cartDetailRepo;
    @Autowired private OrdersRepository orderRepo;
    @Autowired private OrdersDetailRepository orderDetailRepo;

    // --- CẤU HÌNH TỈNH THÀNH (Kho Cần Thơ) ---
    private static final List<String> SOUTH_PROVINCES = Arrays.asList(
        "binh phuoc", "binh duong", "dong nai", "tay ninh", "ba ria", "vung tau",
        "ho chi minh", "sai gon", "hcm", "long an", "dong thap", "tien giang",
        "an giang", "ben tre", "vinh long", "tra vinh", "hau giang", "kien giang",
        "soc trang", "bac lieu", "ca mau"
    );
    private static final List<String> CENTRAL_PROVINCES = Arrays.asList(
        "thanh hoa", "nghe an", "ha tinh", "quang binh", "quang tri", "thua thien hue",
        "da nang", "quang nam", "quang ngai", "binh dinh", "phu yen", "khanh hoa",
        "ninh thuan", "binh thuan", "kon tum", "gia lai", "dak lak", "dak nong", "lam dong"
    );
    private static final List<String> NORTH_PROVINCES = Arrays.asList(
        "lao cai", "yen bai", "dien bien", "hoa binh", "lai chau", "son la", "ha giang",
        "cao bang", "bac kan", "lang son", "tuyen quang", "thai nguyen", "phu tho",
        "bac giang", "quang ninh", "bac ninh", "ha nam", "hai duong", "hai phong",
        "hung yen", "nam dinh", "ninh binh", "thai binh", "vinh phuc", "ha noi", "hn"
    );

    // --- DTO LƯU SESSION ---
    public static class CheckoutData {
        public String address;
        public String phone;
        public String paymentMethod;
        public int totalAmount;
        public int shippingFee;
        public int finalTotal;
        public String tempOrderCode;
    }

    // --- HELPER METHODS ---
    private int calculateShippingFee(String address, int subTotal) {
        if (subTotal >= 1000000) return 0;
        if (address == null || address.isEmpty()) return 50000;
        String normAddress = unAccent(address.toLowerCase());
        if (normAddress.contains("can tho")) return 20000;
        for (String p : SOUTH_PROVINCES) if (normAddress.contains(p)) return 30000;
        for (String p : CENTRAL_PROVINCES) if (normAddress.contains(p)) return 40000;
        for (String p : NORTH_PROVINCES) if (normAddress.contains(p)) return 50000;
        return 45000; 
    }

    public static String unAccent(String s) {
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replaceAll("đ", "d");
    }

    // --- CONTROLLER METHODS ---

    @GetMapping
    public String viewCheckout(Model model) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        List<CartDetail> cartDetails = cartDetailRepo.findByCart_Account_Id(account.getId());
        if (cartDetails.isEmpty()) return "redirect:/cart";

        int subTotal = cartDetails.stream().mapToInt(cd -> cd.getPrice() * cd.getQuantity()).sum();
        int shippingFee = calculateShippingFee(account.getAddress(), subTotal);

        model.addAttribute("cartDetails", cartDetails);
        model.addAttribute("total", subTotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("account", account);

        return "client/checkout";
    }

    @PostMapping
    public String processCheckout(
            @RequestParam("address") String address,
            @RequestParam("phone") String phone,
            @RequestParam("paymentMethod") String paymentMethod,
            Model model) {

        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        List<CartDetail> cartDetails = cartDetailRepo.findByCart_Account_Id(account.getId());
        if (cartDetails.isEmpty()) return "redirect:/cart";

        int subTotal = cartDetails.stream().mapToInt(cd -> cd.getPrice() * cd.getQuantity()).sum();
        int shippingFee = calculateShippingFee(address, subTotal);
        int finalTotal = subTotal + shippingFee;

        // Lưu thông tin vào Session (Chưa lưu DB)
        CheckoutData data = new CheckoutData();
        data.address = address;
        data.phone = phone;
        data.paymentMethod = paymentMethod;
        data.totalAmount = subTotal;
        data.shippingFee = shippingFee;
        data.finalTotal = finalTotal;
        data.tempOrderCode = "DH" + System.currentTimeMillis() / 1000;

        session.setAttribute("checkoutData", data);

        if ("VIETQR".equals(paymentMethod) || "MOMO".equals(paymentMethod)) {
            return "redirect:/checkout/payment";
        }
        return "redirect:/checkout/confirm"; // COD thì xác nhận luôn
    }
    
    @GetMapping("/payment")
    public String viewPaymentQR(Model model) {
        // Lấy dữ liệu từ Session
        CheckoutData data = (CheckoutData) session.getAttribute("checkoutData");
        
        // 🚨 SỬA LỖI QUAN TRỌNG: Kiểm tra null
        // Nếu không có dữ liệu trong session (do truy cập trực tiếp link), đá về giỏ hàng
        if (data == null) {
            return "redirect:/cart";
        }

        String qrUrl = "";
        String bankName = "";
        String accountNo = "";
        String accountName = "";
        String content = data.tempOrderCode; 

        if ("VIETQR".equals(data.paymentMethod)) {
            String BANK_ID = "ICB";
            accountNo = "103878028110";
            accountName = "NGUYEN GIA HUY";
            bankName = "VietinBank";
            qrUrl = String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                BANK_ID, accountNo, data.finalTotal, content, URLEncoder.encode(accountName, StandardCharsets.UTF_8));
        } else if ("MOMO".equals(data.paymentMethod)) {
            String MOMO_PHONE = "0914211221";
            accountNo = MOMO_PHONE;
            accountName = "NGUYEN GIA HUY";
            bankName = "Ví điện tử MoMo";
            String momoLink = String.format("https://me.momo.vn/%s?amount=%d&message=%s",
                MOMO_PHONE, data.finalTotal, URLEncoder.encode(content, StandardCharsets.UTF_8));
            qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=" + URLEncoder.encode(momoLink, StandardCharsets.UTF_8);
        }

        model.addAttribute("qrUrl", qrUrl);
        model.addAttribute("total", data.finalTotal);
        model.addAttribute("bankName", bankName);
        model.addAttribute("accountNo", accountNo);
        model.addAttribute("accountName", accountName);
        model.addAttribute("content", content);
        
        // 🚨 SỬA LỖI QUAN TRỌNG: Truyền object checkoutData sang View
        model.addAttribute("checkoutData", data); 
        
        return "client/payment-qr"; 
    }

    @GetMapping("/confirm")
    public String confirmOrder() {
        Account account = (Account) session.getAttribute("account");
        CheckoutData data = (CheckoutData) session.getAttribute("checkoutData");

        if (account == null || data == null) return "redirect:/cart";

        List<CartDetail> cartDetails = cartDetailRepo.findByCart_Account_Id(account.getId());
        if (cartDetails.isEmpty()) return "redirect:/cart";

        // Lưu đơn hàng chính thức
        Orders order = new Orders();
        order.setAccountId(account);
        order.setCreatedDate(new Date());
        order.setAddress(data.address);
        order.setPhone(data.phone);
        order.setPaymentMethod(data.paymentMethod);
        order.setFeeship(data.shippingFee);
        order.setTotal(data.finalTotal);
        
        if ("COD".equals(data.paymentMethod)) {
            order.setPaymentStatus(false);
            order.setStatus(0); 
        } else {
            order.setPaymentStatus(true); // Đã chuyển khoản
            order.setStatus(1); 
        }

        Orders savedOrder = orderRepo.save(order);

        List<OrderDetail> orderDetails = cartDetails.stream().map(item -> {
            OrderDetail detail = new OrderDetail();
            detail.setOrders(savedOrder);
            detail.setProductId(item.getProduct());
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getPrice());
            return detail;
        }).toList();

        orderDetailRepo.saveAll(orderDetails);
        cartDetailRepo.deleteAll(cartDetails);
        session.removeAttribute("checkoutData"); // Xóa session

        return "redirect:/orders";
    }
}