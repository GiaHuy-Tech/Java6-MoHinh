package com.example.demo.controllers;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.config.VNPayConfig;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.VNPayService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private CartDetailRepository cartDetailRepo;

    @Autowired
    private OrdersRepository ordersRepo;

    @Autowired
    private OrdersDetailRepository orderDetailRepo;

    @Autowired
    private VoucherDetailRepository voucherDetailRepo;

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private AddressRepository addressRepo;

    // THÊM: Inject VNPayService vào đây
    @Autowired
    private VNPayService vnPayService;

    // =====================================================
    // 1️⃣ HIỂN THỊ TRANG CHECKOUT
    // =====================================================
    @GetMapping
    public String viewCheckout(HttpSession session,
                               @RequestParam(required = false) String voucherCode,
                               Model model) {

        Account account = getAccount(session);
        if (account == null) return "redirect:/login";

        List<CartDetail> cartList = cartDetailRepo.findByAccount_Id(account.getId());
        if (cartList.isEmpty()) return "redirect:/cart";

        List<Address> userAddresses = addressRepo.findByAccountId(account.getId());
        model.addAttribute("addresses", userAddresses);

        List<VoucherDetail> myVouchers = voucherDetailRepo.findByAccount_Id(account.getId())
                .stream()
                .filter(v -> !Boolean.TRUE.equals(v.getIsUsed()))
                .collect(Collectors.toList());
        model.addAttribute("savedVouchers", myVouchers);

        BigDecimal rawTotal = BigDecimal.ZERO;
        for (CartDetail item : cartList) {
            rawTotal = rawTotal.add(item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal feeShip = BigDecimal.ZERO;

        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            Optional<VoucherDetail> voucherOpt = voucherDetailRepo.findValidVoucherForAccount(account.getId(), voucherCode.trim());
            if (voucherOpt.isPresent()) {
                Voucher v = voucherOpt.get().getVoucher();
                if (v.getDiscountPercent() != null && v.getDiscountPercent() > 0) {
                    discount = rawTotal.multiply(BigDecimal.valueOf(v.getDiscountPercent()).divide(BigDecimal.valueOf(100)));
                } else if (v.getDiscountAmount() != null && v.getDiscountAmount() > 0) {
                    discount = BigDecimal.valueOf(v.getDiscountAmount());
                }
                if (discount.compareTo(rawTotal) > 0) discount = rawTotal;
                if (Boolean.TRUE.equals(v.getIsFreeShipping())) feeShip = BigDecimal.ZERO;
            }
        }

        BigDecimal finalTotal = rawTotal.subtract(discount).add(feeShip);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) finalTotal = BigDecimal.ZERO;

        model.addAttribute("cartList", cartList);
        model.addAttribute("rawTotal", rawTotal);
        model.addAttribute("discount", discount);
        model.addAttribute("feeShip", feeShip);
        model.addAttribute("finalTotal", finalTotal);
        model.addAttribute("voucherCode", voucherCode);

        return "client/checkout";
    }

    // =====================================================
    // 2️⃣ XÁC NHẬN ĐẶT HÀNG (LƯU DB & GỌI VNPAY NẾU CẦN)
    // =====================================================
    @PostMapping("/confirm")
    public String confirmOrder(HttpSession session,
                               @RequestParam(required = false) String voucherCode,
                               @RequestParam("addressId") Long addressId,
                               @RequestParam("paymentMethod") String paymentMethod) {

        Account account = getAccount(session);
        if (account == null) return "redirect:/login";

        List<CartDetail> cartList = cartDetailRepo.findByAccount_Id(account.getId());
        if (cartList.isEmpty()) return "redirect:/cart";

        Address selectedAddress = addressRepo.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ không tồn tại"));

        BigDecimal rawTotal = BigDecimal.ZERO;
        for (CartDetail item : cartList) {
            rawTotal = rawTotal.add(item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal feeShip = BigDecimal.ZERO;
        VoucherDetail appliedVoucherDetail = null;

        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            Optional<VoucherDetail> voucherOpt = voucherDetailRepo.findValidVoucherForAccount(account.getId(), voucherCode.trim());
            if (voucherOpt.isPresent()) {
                appliedVoucherDetail = voucherOpt.get();
                Voucher v = appliedVoucherDetail.getVoucher();
                if (v.getDiscountPercent() != null) {
                    discount = rawTotal.multiply(BigDecimal.valueOf(v.getDiscountPercent()).divide(BigDecimal.valueOf(100)));
                } else if (v.getDiscountAmount() != null) {
                    discount = BigDecimal.valueOf(v.getDiscountAmount());
                }
                if (discount.compareTo(rawTotal) > 0) discount = rawTotal;
                if (Boolean.TRUE.equals(v.getIsFreeShipping())) feeShip = BigDecimal.ZERO;
            }
        }

        BigDecimal finalTotal = rawTotal.subtract(discount).add(feeShip);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) finalTotal = BigDecimal.ZERO;

        // BƯỚC 1: LƯU ORDER VỚI TRẠNG THÁI "CHƯA THANH TOÁN"
        Orders order = new Orders();
        order.setAccount(account);
        order.setCreatedDate(new Date());
        order.setTotal(finalTotal);
        order.setFeeship(feeShip);
        order.setMoneyDiscounted(discount);
        
        // 0: Chờ xác nhận/Chưa thanh toán
        order.setStatus(0); 
        order.setPaymentStatus(false); 
        
        if (appliedVoucherDetail != null) {
            order.setVoucherCode(appliedVoucherDetail.getVoucher().getCode());
        }

        // Lưu đơn hàng để sinh ra ID
        ordersRepo.save(order);

        // Lưu Order Detail
        for (CartDetail item : cartList) {
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(item.getProduct());
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getProduct().getPrice());
            orderDetailRepo.save(detail);
        }

        // Cập nhật Voucher (nếu có)
        if (appliedVoucherDetail != null) {
            appliedVoucherDetail.setIsUsed(true);
            appliedVoucherDetail.setUsedAt(new Date());
            appliedVoucherDetail.setStatus("USED");
            voucherDetailRepo.save(appliedVoucherDetail);
        }

        // Xoá giỏ hàng
        cartDetailRepo.deleteAll(cartList);

        // =====================================================
        // BƯỚC 2: KIỂM TRA PHƯƠNG THỨC THANH TOÁN
        // =====================================================
        if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            // Ép kiểu tổng tiền sang int để truyền vào service VNPAY
            int total = finalTotal.intValue();
            
            // Dùng ID của đơn hàng làm OrderInfo để lát nữa VNPAY trả về mình biết là đơn nào
            String orderInfo = String.valueOf(order.getId()); 
            
            // Gọi hàm tạo URL thanh toán
            String paymentUrl = vnPayService.createOrder(total, orderInfo, VNPayConfig.vnp_ReturnUrl);
            
            // Chuyển hướng người dùng sang trang của VNPAY
            return "redirect:" + paymentUrl;
        }

        // Nếu thanh toán COD (Nhận hàng trả tiền) thì chuyển về trang lịch sử đơn hàng luôn
        return "redirect:/orders";
    }

    // =====================================================
    // 3️⃣ HỨNG KẾT QUẢ TỪ VNPAY TRẢ VỀ
    // =====================================================
    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request, Model model) {
        // Kiểm tra mã hash và kết quả giao dịch từ VNPayService
        int paymentStatus = vnPayService.orderReturn(request);

        // Lấy lại ID đơn hàng từ orderInfo mà ta đã truyền lúc nãy
        String orderInfo = request.getParameter("vnp_OrderInfo");
        Integer orderId = Integer.valueOf(orderInfo);
        // Tìm lại đơn hàng trong Database
        Orders order = ordersRepo.findById(orderId).orElse(null);

        if (paymentStatus == 1) { // 1 = GIAO DỊCH THÀNH CÔNG
            if (order != null) {
                // Cập nhật trạng thái đã thanh toán
                order.setPaymentStatus(true);
                order.setStatus(1); // 1: Đã xác nhận/Đã thanh toán
                ordersRepo.save(order);
                
                // Cộng tiền vào tổng chi tiêu của Account
                Account acc = order.getAccount();
                BigDecimal current = acc.getTotalSpending() == null ? BigDecimal.ZERO : acc.getTotalSpending();
                acc.setTotalSpending(current.add(order.getTotal()));
                accountRepo.save(acc);
            }
            // Bạn có thể tạo 1 trang payment-success.html để hiển thị cho đẹp
            return "redirect:/orders"; 
            
        } else { // 0 hoặc -1 = GIAO DỊCH THẤT BẠI / SAI CHỮ KÝ
            if (order != null) {
                // Đánh dấu đơn hàng là đã huỷ hoặc thanh toán lỗi
                order.setStatus(3); // 3: Đã huỷ
                ordersRepo.save(order);
            }
            // Bạn có thể tạo 1 trang payment-fail.html để báo lỗi
            return "redirect:/cart"; 
        }
    }

    // Hàm phụ trợ lấy thông tin user đang đăng nhập
    private Account getAccount(HttpSession session) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) account = (Account) session.getAttribute("user");
        return account;
    }
}