package com.example.demo.controllers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Autowired private CartDetailRepository cartDetailRepo;
    @Autowired private OrdersRepository ordersRepo;
    @Autowired private OrdersDetailRepository orderDetailRepo;
    @Autowired private VoucherDetailRepository voucherDetailRepo;
    @Autowired private VoucherRepository voucherRepo; // Dùng để lấy voucher chung
    @Autowired private AccountRepository accountRepo;
    @Autowired private AddressRepository addressRepo;
    @Autowired private VNPayService vnPayService;

    @GetMapping
    public String viewCheckout(HttpSession session,
                               @RequestParam(required = false) String voucherCode,
                               Model model) {
        Account account = getAccount(session);
        if (account == null) return "redirect:/login";

        List<CartDetail> cartList = cartDetailRepo.findByAccount_Id(account.getId());
        if (cartList.isEmpty()) return "redirect:/cart";

        // 1. Tính tổng tiền hàng (Raw Total)
        BigDecimal rawTotal = cartList.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. LẤY DANH SÁCH VOUCHER KHẢ DỤNG (Đã sửa để đổ dữ liệu lên)
        // Lấy voucher riêng đã thu thập
        List<VoucherDetail> myVoucherDetails = voucherDetailRepo.findByAccount_IdAndIsUsedFalse(account.getId());
        // Lấy thêm voucher chung (account_id is null)
        List<Voucher> publicVouchers = voucherRepo.findByAccountIsNull();

        List<Voucher> availableVouchers = new ArrayList<>();
        
        // Gộp voucher riêng
        myVoucherDetails.forEach(vd -> availableVouchers.add(vd.getVoucher()));
        // Gộp voucher chung
        availableVouchers.addAll(publicVouchers);

        // Lọc theo điều kiện: Còn hạn + Đủ giá trị đơn hàng tối thiểu
        LocalDateTime now = LocalDateTime.now(); // Lấy thời gian hiện tại

        List<Voucher> savedVouchers = availableVouchers.stream()
                .filter(v -> v != null && Boolean.TRUE.equals(v.getActive()))
                // THÊM DÒNG NÀY: Kiểm tra nếu expiredAt null thì coi như vô hạn, hoặc phải còn hạn
                .filter(v -> v.getExpiredAt() == null || v.getExpiredAt().isAfter(now)) 
                .filter(v -> v.getMinOrderValue() == null || rawTotal.doubleValue() >= v.getMinOrderValue())
                .distinct()
                .collect(Collectors.toList());
        model.addAttribute("savedVouchers", savedVouchers);

        // 3. XỬ LÝ ÁP DỤNG VOUCHER
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal feeShip = BigDecimal.valueOf(30000);

        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            Optional<Voucher> vOpt = savedVouchers.stream()
                    .filter(v -> v.getCode().equals(voucherCode.trim()))
                    .findFirst();

            if (vOpt.isPresent()) {
                Voucher v = vOpt.get();
                if (v.getDiscountPercent() != null) {
                    discount = rawTotal.multiply(BigDecimal.valueOf(v.getDiscountPercent()).divide(BigDecimal.valueOf(100)));
                } else if (v.getDiscountAmount() != null) {
                    discount = BigDecimal.valueOf(v.getDiscountAmount());
                }
                // Giảm giá không vượt quá tổng tiền
                if (discount.compareTo(rawTotal) > 0) discount = rawTotal;
                if (Boolean.TRUE.equals(v.getIsFreeShipping())) feeShip = BigDecimal.ZERO;
            }
        }

        BigDecimal finalTotal = rawTotal.subtract(discount).add(feeShip);

        model.addAttribute("cartList", cartList);
        model.addAttribute("addresses", addressRepo.findByAccountId(account.getId()));
        model.addAttribute("rawTotal", rawTotal);
        model.addAttribute("discount", discount);
        model.addAttribute("feeShip", feeShip);
        model.addAttribute("finalTotal", finalTotal);
        model.addAttribute("voucherCode", voucherCode);
        model.addAttribute("user", account);

        return "client/checkout";
    }

    @PostMapping("/confirm")
    public String confirmOrder(HttpSession session,
                               @RequestParam(required = false) String voucherCode,
                               @RequestParam("addressId") Long addressId,
                               @RequestParam("paymentMethod") String paymentMethod) {
        Account account = getAccount(session);
        if (account == null) return "redirect:/login";

        List<CartDetail> cartList = cartDetailRepo.findByAccount_Id(account.getId());
        if (cartList.isEmpty()) return "redirect:/cart";

        // 1. Tính tổng tiền hàng gốc
        BigDecimal rawTotal = cartList.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal feeShip = BigDecimal.valueOf(30000); // Mặc định
        Voucher appliedVoucher = null;

        // 2. TÌM VOUCHER ĐỂ TÍNH LẠI GIẢM GIÁ (Tìm cả chung và riêng)
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            // Tìm trong bảng Voucher gốc (Bao gồm cả voucher chung và voucher riêng)
            Optional<Voucher> vOpt = voucherRepo.findAll().stream()
                    .filter(v -> v.getCode().equals(voucherCode.trim()) && Boolean.TRUE.equals(v.getActive()))
                    .findFirst();

            if (vOpt.isPresent()) {
                appliedVoucher = vOpt.get();
                
                // Tính số tiền giảm
                if (appliedVoucher.getDiscountPercent() != null) {
                    discount = rawTotal.multiply(BigDecimal.valueOf(appliedVoucher.getDiscountPercent()).divide(BigDecimal.valueOf(100)));
                } else if (appliedVoucher.getDiscountAmount() != null) {
                    discount = BigDecimal.valueOf(appliedVoucher.getDiscountAmount());
                }

                // Giảm giá không được vượt quá tiền hàng
                if (discount.compareTo(rawTotal) > 0) discount = rawTotal;
                
                // Kiểm tra miễn phí vận chuyển
                if (Boolean.TRUE.equals(appliedVoucher.getIsFreeShipping())) {
                    feeShip = BigDecimal.ZERO;
                }

                // 3. CẬP NHẬT TRẠNG THÁI VOUCHER (Nếu là voucher riêng của User)
                Optional<VoucherDetail> vdOpt = voucherDetailRepo.findValidVoucherForAccount(account.getId(), voucherCode.trim());
                if (vdOpt.isPresent()) {
                    VoucherDetail vd = vdOpt.get();
                    vd.setIsUsed(true);
                    vd.setUsedAt(new Date());
                    vd.setStatus("USED");
                    voucherDetailRepo.save(vd);
                }
            }
        }

        // 4. Tính tổng tiền cuối cùng cực kỳ quan trọng
        BigDecimal finalTotal = rawTotal.subtract(discount).add(feeShip);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) finalTotal = BigDecimal.ZERO;

        // 5. Lưu Đơn hàng với finalTotal đã giảm
        Orders order = new Orders();
        order.setAccount(account);
        order.setCreatedDate(new Date());
        order.setTotal(finalTotal); // <--- TIỀN ĐÃ GIẢM LƯU TẠI ĐÂY
        order.setFeeship(feeShip);
        order.setMoneyDiscounted(discount);
        order.setStatus(0);
        order.setPaymentStatus(false);
        order.setVoucherCode(voucherCode);
        ordersRepo.save(order);

        // 6. Lưu Chi tiết đơn hàng
        for (CartDetail item : cartList) {
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(item.getProduct());
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getProduct().getPrice());
            orderDetailRepo.save(detail);
        }

        cartDetailRepo.deleteAll(cartList);

        // 7. CHUYỂN HƯỚNG THANH TOÁN
        if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            // Truyền finalTotal (đã giảm) vào VNPay
            String paymentUrl = vnPayService.createOrder(finalTotal.intValue(), String.valueOf(order.getId()), VNPayConfig.vnp_ReturnUrl);
            return "redirect:" + paymentUrl;
        }

        return "redirect:/orders";
    }

    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request) {
        int paymentStatus = vnPayService.orderReturn(request);
        String orderIdStr = request.getParameter("vnp_OrderInfo");
        
        if (orderIdStr != null) {
            Orders order = ordersRepo.findById(Integer.valueOf(orderIdStr)).orElse(null);
            if (order != null && paymentStatus == 1) {
                order.setPaymentStatus(true);
                order.setStatus(1);
                ordersRepo.save(order);
                return "redirect:/orders?success";
            }
        }
        return "redirect:/cart?error";
    }

    private Account getAccount(HttpSession session) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) account = (Account) session.getAttribute("user");
        return account;
    }
}