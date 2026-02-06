package com.example.demo.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.model.Account;
import com.example.demo.model.CartDetail;
import com.example.demo.model.OrderDetail;
import com.example.demo.model.Orders;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.CartDetailRepository;
import com.example.demo.repository.OrdersDetailRepository;
import com.example.demo.repository.OrdersRepository;
import com.example.demo.service.MembershipService;
import com.example.demo.service.VNPayService; // ✅ Import Service VNPay

import jakarta.servlet.http.HttpServletRequest; // ✅ Cần cho VNPay
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired private HttpSession session;
    @Autowired private CartDetailRepository cartDetailRepo;
    @Autowired private OrdersRepository orderRepo;
    @Autowired private OrdersDetailRepository orderDetailRepo;
    @Autowired private AccountRepository accountRepo;
    @Autowired private MembershipService membershipService;
    
    // ✅ Inject VNPayService đã tạo ở bước trước
    @Autowired private VNPayService vnPayService;

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

        int subTotal = cartDetails.stream().mapToInt(cd -> cd.getPrice() * cd.getQuantity()).sum();
        int shippingFee = calculateShippingFee(account.getAddress(), subTotal);
        int discountPercent = membershipService.getDiscountPercent(account.getMembershipLevel());
        int discountAmount = (int) (subTotal * discountPercent / 100.0);
        int finalTotal = subTotal - discountAmount + shippingFee;

        model.addAttribute("cartDetails", cartDetails);
        model.addAttribute("subTotal", subTotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("discountPercent", discountPercent);
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("total", finalTotal);
        model.addAttribute("account", account);

        return "client/checkout";
    }

    @PostMapping
    public String processCheckout(
            @RequestParam("address") String address,
            @RequestParam("phone") String phone,
            @RequestParam("paymentMethod") String paymentMethod,
            HttpServletRequest request, // ✅ Thêm request để lấy base URL cho VNPay
            Model model) {

        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";
        
        // Refresh account
        account = accountRepo.findById(account.getId()).orElse(account);

        List<CartDetail> cartDetails = cartDetailRepo.findByCart_Account_Id(account.getId());
        if (cartDetails.isEmpty()) return "redirect:/cart";

        int subTotal = cartDetails.stream().mapToInt(cd -> cd.getPrice() * cd.getQuantity()).sum();
        int shippingFee = calculateShippingFee(address, subTotal);
        int discountPercent = membershipService.getDiscountPercent(account.getMembershipLevel());
        int discountAmount = (int) (subTotal * discountPercent / 100.0);
        int finalTotal = subTotal - discountAmount + shippingFee;

        // Tạo đơn hàng
        Orders order = new Orders();
        order.setAccountId(account);
        order.setCreatedDate(new Date());
        order.setAddress(address);
        order.setPhone(phone);
        order.setPaymentMethod(paymentMethod);
        order.setFeeship(shippingFee);
        order.setTotal(finalTotal);
        
        // Tạo mã đơn hàng duy nhất để mapping với VNPay
        String uniqueOrderCode = "DH" + System.currentTimeMillis();
        order.setNote(uniqueOrderCode); 

        order.setPaymentStatus(false);
        order.setStatus(0);

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

        // --- XỬ LÝ THANH TOÁN ---

        // 1. THANH TOÁN VNPAY
        if ("VNPAY".equals(paymentMethod)) {
            // Tạo URL trả về (http://localhost:8080/checkout/vnpay-return)
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String vnpayUrl = vnPayService.createOrder(finalTotal, uniqueOrderCode, baseUrl + "/checkout/vnpay-return");
            return "redirect:" + vnpayUrl;
        }

        // 2. COD (Thanh toán khi nhận hàng)
        if ("COD".equals(paymentMethod)) {
            updateMembershipSpending(account, finalTotal);
            return "redirect:/orders";
        } 
        
        // 3. QR / MOMO (Chuyển khoản thủ công)
        else {
            session.setAttribute("pendingOrderId", savedOrder.getId());
            return "redirect:/checkout/payment";
        }
    }

    // --- ✅ XỬ LÝ KẾT QUẢ TRẢ VỀ TỪ VNPAY ---
    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request, Model model){
        // 1. Kiểm tra chữ ký bảo mật (Checksum)
        int paymentStatus = vnPayService.orderReturn(request);

        String orderInfo = request.getParameter("vnp_OrderInfo"); // Đây chính là uniqueOrderCode ta đã gửi đi
        String totalPrice = request.getParameter("vnp_Amount");
        String transactionId = request.getParameter("vnp_TransactionNo");

        model.addAttribute("orderId", orderInfo);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("transactionId", transactionId);

        if(paymentStatus == 1){
            // --- GIAO DỊCH THÀNH CÔNG ---
            
            // Tìm đơn hàng bằng mã ghi chú (Dùng hàm findByNote trong Repo bạn vừa sửa)
            Optional<Orders> orderOpt = orderRepo.findByNote(orderInfo);
            
            if(orderOpt.isPresent()) {
                Orders order = orderOpt.get();
                
                // Kiểm tra để tránh cộng điểm 2 lần nếu user f5 trang
                if (!Boolean.TRUE.equals(order.getPaymentStatus())) {
                    order.setPaymentStatus(true);
                    order.setStatus(1); // Đã xác nhận
                    orderRepo.save(order);
                    
                    // Cộng điểm thành viên
                    updateMembershipSpending(order.getAccountId(), order.getTotal());
                }
            }
            
            return "client/order-success"; // Bạn cần tạo file templates/client/order-success.html
        } else {
            // --- GIAO DỊCH THẤT BẠI ---
            return "client/order-fail";   // Bạn cần tạo file templates/client/order-fail.html
        }
    }

    @GetMapping("/payment")
    public String viewPaymentQR(Model model) {
        Integer orderId = (Integer) session.getAttribute("pendingOrderId");
        if (orderId == null) return "redirect:/cart";

        Orders order = orderRepo.findById(orderId).orElse(null);
        if(order == null) return "redirect:/cart";

        if (Boolean.TRUE.equals(order.getPaymentStatus())) return "redirect:/orders";

        String content = order.getNote();
        String qrUrl = "";
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

    @PostMapping("/confirm-payment")
    public String confirmPaymentManual() {
        Integer orderId = (Integer) session.getAttribute("pendingOrderId");
        if (orderId != null) {
            Orders order = orderRepo.findById(orderId).orElse(null);
            if (order != null) {
                order.setPaymentStatus(true);
                order.setStatus(1);
                orderRepo.save(order);
                if (order.getAccountId() != null) {
                    updateMembershipSpending(order.getAccountId(), order.getTotal());
                }
            }
            session.removeAttribute("pendingOrderId");
        }
        return "redirect:/orders";
    }

    // --- HELPER: CẬP NHẬT ĐIỂM VÀ HẠNG ---
    private void updateMembershipSpending(Account account, int amountToAdd) {
        long currentSpending = account.getTotalSpending() == null ? 0 : account.getTotalSpending();
        account.setTotalSpending(currentSpending + amountToAdd);
        membershipService.updateMembershipLevel(account); // Service tính toán hạng
        accountRepo.save(account);
        session.setAttribute("account", account); // Update session
    }
}