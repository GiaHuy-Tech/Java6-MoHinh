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

    // --- CẤU HÌNH TỈNH THÀNH ---
    private static final List<String> SOUTH_PROVINCES = Arrays.asList(
        "ho chi minh", "hcm", "sai gon", "can tho", "binh duong", "dong nai", "long an", 
        "tien giang", "ben tre", "tra vinh", "vinh long", "dong thap", "an giang", 
        "kien giang", "hau giang", "soc trang", "bac lieu", "ca mau", "ba ria", 
        "vung tau", "tay ninh", "binh phuoc"
    );

    // --- HELPER METHODS ---
    public static String unAccent(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replaceAll("đ", "d");
    }

    private int calculateShippingFee(String address, int subTotal) {
        if (subTotal >= 1000000) return 0; 
        if (address == null || address.trim().isEmpty()) return 50000;

        String normAddress = unAccent(address.toLowerCase());
        
        if (normAddress.contains("can tho")) return 0; // Cần Thơ Free Ship
        for (String p : SOUTH_PROVINCES) {
            if (normAddress.contains(p)) return 30000;
        }
        return 45000; 
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
        model.addAttribute("keyword", ""); 
        model.addAttribute("selectedCategory", "");

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

        // Lưu đơn hàng
        Orders order = new Orders();
        order.setAccountId(account);
        order.setCreatedDate(new Date());
        order.setAddress(address);
        order.setPhone(phone);
        order.setPaymentMethod(paymentMethod);
        order.setFeeship(shippingFee);
        order.setTotal(finalTotal);
        
        String uniqueOrderCode = "DH" + System.currentTimeMillis(); 
        order.setNote(uniqueOrderCode); 

        order.setPaymentStatus(false);
        order.setStatus(0); 

        Orders savedOrder = orderRepo.save(order);

        // Lưu chi tiết
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

        if ("COD".equals(paymentMethod)) {
            return "redirect:/orders"; 
        } else {
            session.setAttribute("pendingOrderId", savedOrder.getId());
            return "redirect:/checkout/payment";
        }
    }

    @GetMapping("/payment")
    public String viewPaymentQR(Model model) {
        Integer orderId = (Integer) session.getAttribute("pendingOrderId");
        if (orderId == null) return "redirect:/cart";

        Orders order = orderRepo.findById(orderId).orElse(null);
        if(order == null) return "redirect:/cart";

        if (Boolean.TRUE.equals(order.getPaymentStatus())) {
             return "redirect:/orders"; 
        }

        String content = order.getNote(); 
        String qrUrl = "";
        
        // --- SỬA LẠI INFO BANK CỦA BẠN Ở ĐÂY ---
        String bankId = "ICB"; 
        String accountNo = "103878028110";
        String accountName = "NGUYEN GIA HUY";

        if ("VIETQR".equals(order.getPaymentMethod())) {
             qrUrl = String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                bankId, accountNo, order.getTotal(), content, URLEncoder.encode(accountName, StandardCharsets.UTF_8));
        } 
        else if ("MOMO".equals(order.getPaymentMethod())) {
            String MOMO_PHONE = "0914211221";
            String momoLink = String.format("https://me.momo.vn/%s?amount=%d&message=%s",
                 MOMO_PHONE, order.getTotal(), URLEncoder.encode(content, StandardCharsets.UTF_8));
            qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=" + URLEncoder.encode(momoLink, StandardCharsets.UTF_8);
        }

        model.addAttribute("qrUrl", qrUrl);
        model.addAttribute("total", order.getTotal());
        model.addAttribute("content", content);
        model.addAttribute("bankName", "VietinBank");
        model.addAttribute("accountNo", accountNo);
        model.addAttribute("accountName", accountName);
        model.addAttribute("paymentMethod", order.getPaymentMethod()); 
        
        return "client/payment-qr"; 
    }
    
    // API Ajax check status (Vẫn giữ để ai dùng Webhook thì dùng)
    @GetMapping("/check-status")
    @ResponseBody
    public Map<String, Boolean> checkOrderStatus() {
        Integer orderId = (Integer) session.getAttribute("pendingOrderId");
        Map<String, Boolean> response = new HashMap<>();
        
        if (orderId != null) {
            Orders order = orderRepo.findById(orderId).orElse(null);
            if (order != null && Boolean.TRUE.equals(order.getPaymentStatus())) {
                response.put("paid", true);
                return response;
            }
        }
        response.put("paid", false);
        return response;
    }

    // --- MỚI THÊM: XỬ LÝ NÚT "TÔI ĐÃ CHUYỂN TIỀN" ---
    @PostMapping("/confirm-payment")
    public String confirmPaymentManual() {
        Integer orderId = (Integer) session.getAttribute("pendingOrderId");
        
        if (orderId != null) {
            Orders order = orderRepo.findById(orderId).orElse(null);
            if (order != null) {
                // Cập nhật trạng thái thành Đã thanh toán
                order.setPaymentStatus(false);
                // Cập nhật trạng thái đơn thành Đang xử lý
                order.setStatus(0); 
                orderRepo.save(order);
            }
            // Xóa session để hoàn tất
            session.removeAttribute("pendingOrderId");
        }
        
        return "redirect:/orders";
    }
}