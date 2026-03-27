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
        BigDecimal feeShip = BigDecimal.valueOf(30000); // Phí mặc định
        Voucher appliedVoucher = null;

        // 2. TÌM VOUCHER ĐỂ TÍNH LẠI GIẢM GIÁ
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            Optional<Voucher> vOpt = voucherRepo.findAll().stream()
                    .filter(v -> v.getCode().equals(voucherCode.trim()) && Boolean.TRUE.equals(v.getActive()))
                    .findFirst();

            if (vOpt.isPresent()) {
                appliedVoucher = vOpt.get();
                if (appliedVoucher.getDiscountPercent() != null) {
                    discount = rawTotal.multiply(BigDecimal.valueOf(appliedVoucher.getDiscountPercent()).divide(BigDecimal.valueOf(100)));
                } else if (appliedVoucher.getDiscountAmount() != null) {
                    discount = BigDecimal.valueOf(appliedVoucher.getDiscountAmount());
                }
                if (discount.compareTo(rawTotal) > 0) discount = rawTotal;
                if (Boolean.TRUE.equals(appliedVoucher.getIsFreeShipping())) feeShip = BigDecimal.ZERO;

                // Cập nhật trạng thái voucher riêng (nếu có)
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

        // 3. Tính tổng tiền cuối cùng
        BigDecimal finalTotal = rawTotal.subtract(discount).add(feeShip);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) finalTotal = BigDecimal.ZERO;

        // 4. TÌM ĐỊA CHỈ TỪ DATABASE (Phần quan trọng nhất bị thiếu)
        // Tìm địa chỉ và đảm bảo địa chỉ này thuộc về account đang đăng nhập
        Address selectedAddress = addressRepo.findById(addressId)
                .filter(a -> a.getAccount().getId().equals(account.getId()))
                .orElse(null);

        // 5. LƯU ĐƠN HÀNG
        Orders order = new Orders();
        order.setAccount(account);
        order.setCreatedDate(new Date());
        
        // GÁN ĐỊA CHỈ VÀO ĐƠN HÀNG
        order.setAddress(selectedAddress); 
        
        // Gán số điện thoại (Ưu tiên lấy từ địa chỉ giao hàng)
        if (selectedAddress != null) {
            order.setPhone(selectedAddress.getRecipientPhone());
        }

        order.setTotal(finalTotal);
        order.setFeeship(feeShip);
        order.setMoneyDiscounted(discount);
        order.setStatus(0); // Chờ xác nhận
        order.setPaymentStatus(false);
        order.setVoucherCode(voucherCode);
        order.setPaymentMethod(paymentMethod);
        
        ordersRepo.save(order); // Lưu order để lấy được ID cho OrderDetail

        // 6. Lưu Chi tiết đơn hàng
        for (CartDetail item : cartList) {
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(item.getProduct());
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getProduct().getPrice());
            orderDetailRepo.save(detail);
        }

        // Xóa giỏ hàng sau khi đặt hàng thành công
        cartDetailRepo.deleteAll(cartList);

        // 7. CHUYỂN HƯỚNG THANH TOÁN
        if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
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