package com.example.demo.controllers;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.MembershipService;

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
    @Autowired private AccountRepository accountRepo; // ✅ Cần repo để update user
    @Autowired private MembershipService membershipService; // ✅ Inject Service xử lý hạng

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
        
        if (normAddress.contains("can tho")) return 0;
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

        // 1. Tính tổng tiền hàng
        int subTotal = cartDetails.stream().mapToInt(cd -> cd.getPrice() * cd.getQuantity()).sum();
        
        // 2. Tính phí ship
        int shippingFee = calculateShippingFee(account.getAddress(), subTotal);

        // 3. ✅ Tính giảm giá thành viên
        int discountPercent = membershipService.getDiscountPercent(account.getMembershipLevel());
        int discountAmount = (int) (subTotal * discountPercent / 100.0);

        // 4. Tổng cuối cùng
        int finalTotal = subTotal - discountAmount + shippingFee;

        model.addAttribute("cartDetails", cartDetails);
        model.addAttribute("subTotal", subTotal); // Tổng tiền hàng chưa giảm
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("discountPercent", discountPercent); // % Giảm
        model.addAttribute("discountAmount", discountAmount);   // Số tiền giảm
        model.addAttribute("total", finalTotal); // Tổng thanh toán
        
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

        // Reload account từ DB để đảm bảo dữ liệu mới nhất
        account = accountRepo.findById(account.getId()).orElse(account);

        List<CartDetail> cartDetails = cartDetailRepo.findByCart_Account_Id(account.getId());
        if (cartDetails.isEmpty()) return "redirect:/cart";

        // Tính toán lại Server-side (Bảo mật)
        int subTotal = cartDetails.stream().mapToInt(cd -> cd.getPrice() * cd.getQuantity()).sum();
        int shippingFee = calculateShippingFee(address, subTotal);
        
        // ✅ Áp dụng giảm giá
        int discountPercent = membershipService.getDiscountPercent(account.getMembershipLevel());
        int discountAmount = (int) (subTotal * discountPercent / 100.0);
        int finalTotal = subTotal - discountAmount + shippingFee;

        // Lưu đơn hàng
        Orders order = new Orders();
        order.setAccountId(account);
        order.setCreatedDate(new Date());
        order.setAddress(address);
        order.setPhone(phone);
        order.setPaymentMethod(paymentMethod);
        order.setFeeship(shippingFee);
        order.setTotal(finalTotal); // Lưu số tiền sau khi đã giảm giá
        
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

        // ✅ XỬ LÝ CỘNG TIỀN TÍCH LŨY
        if ("COD".equals(paymentMethod)) {
            // Đối với COD, giả định đơn thành công thì cộng điểm luôn (hoặc chờ admin duyệt)
            // Ở đây demo mình cộng luôn để thấy kết quả
            updateMembershipSpending(account, finalTotal);
            return "redirect:/orders"; 
        } else {
            // Chuyển khoản thì chưa cộng tiền vội, chờ xác nhận ở trang payment
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
        
        // --- INFO BANK ---
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
    
    // API Ajax check status
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

    // --- XỬ LÝ XÁC NHẬN THANH TOÁN (ONLINE) ---
    @PostMapping("/confirm-payment")
    public String confirmPaymentManual() {
        Integer orderId = (Integer) session.getAttribute("pendingOrderId");
        
        if (orderId != null) {
            Orders order = orderRepo.findById(orderId).orElse(null);
            if (order != null) {
                // 1. Cập nhật trạng thái đơn hàng
                order.setPaymentStatus(true); // Đã thanh toán
                order.setStatus(1); // Đã xác nhận (Ví dụ)
                orderRepo.save(order);

                // 2. ✅ CỘNG TIỀN VÀO TÀI KHOẢN & UPDATE HẠNG
                Account account = order.getAccountId();
                if (account != null) {
                    updateMembershipSpending(account, order.getTotal());
                }
            }
            // Xóa session
            session.removeAttribute("pendingOrderId");
        }
        
        return "redirect:/orders";
    }

    // --- ✅ HÀM PRIVATE HỖ TRỢ CỘNG TIỀN ---
    private void updateMembershipSpending(Account account, int amountToAdd) {
        // Cộng tiền
        long currentSpending = account.getTotalSpending() == null ? 0 : account.getTotalSpending();
        account.setTotalSpending(currentSpending + amountToAdd);

        // Tính lại hạng
        membershipService.updateMembershipLevel(account);
        
        // Lưu vào DB
        accountRepo.save(account);
        
        // Cập nhật lại session để hiển thị ngay trên Header
        session.setAttribute("account", account);
    }
}