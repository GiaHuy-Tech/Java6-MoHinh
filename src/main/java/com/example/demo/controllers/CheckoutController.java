package com.example.demo.controllers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.config.VNPayConfig;
import com.example.demo.model.Account;
import com.example.demo.model.Address;
import com.example.demo.model.CartDetail;
import com.example.demo.model.OrderDetail;
import com.example.demo.model.Orders;
import com.example.demo.model.Products;
import com.example.demo.model.Voucher;
import com.example.demo.model.VoucherDetail;
import com.example.demo.repository.AddressRepository;
import com.example.demo.repository.CartDetailRepository;
import com.example.demo.repository.OrdersDetailRepository;
import com.example.demo.repository.OrdersRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.VoucherDetailRepository;
import com.example.demo.repository.VoucherRepository;
import com.example.demo.service.ShippingService;
import com.example.demo.service.VNPayService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired private CartDetailRepository cartDetailRepo;
    @Autowired private OrdersRepository ordersRepo;
    @Autowired private OrdersDetailRepository orderDetailRepo;
    @Autowired private VoucherDetailRepository voucherDetailRepo;
    @Autowired private VoucherRepository voucherRepo;
    @Autowired private AddressRepository addressRepo;
    @Autowired private VNPayService vnPayService;
    @Autowired private ShippingService shippingService;
    @Autowired private ProductRepository productRepo;

    // ================== VIEW CHECKOUT ==================
    @GetMapping
    public String viewCheckout(HttpSession session,
                               @RequestParam(required = false) String voucherCode,
                               @RequestParam(required = false) Long addressId,
                               Model model) {

        Account account = getAccount(session);
        if (account == null) {
            return "redirect:/login";
        }

        List<CartDetail> cartList = cartDetailRepo.findByAccount_Id(account.getId());
        if (cartList.isEmpty()) {
            return "redirect:/cart";
        }

        BigDecimal rawTotal = cartList.stream()
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ===== ADDRESS =====
        List<Address> addresses = addressRepo.findByAccount_Id(account.getId());
        Address selectedAddress = null;

        if (addressId != null) {
            selectedAddress = addressRepo.findByIdAndAccount_Id(addressId, account.getId()).orElse(null);
        }
        if (selectedAddress == null) {
            selectedAddress = addresses.stream().filter(a -> Boolean.TRUE.equals(a.getIsDefault())).findFirst().orElse(null);
        }

        // ===== SHIPPING =====
        BigDecimal feeShip = shippingService.calculateFee(selectedAddress, cartList, account);

        // ===== TÍNH GIẢM GIÁ THÀNH VIÊN (MEMBERSHIP DISCOUNT) =====
        BigDecimal memDiscount = BigDecimal.ZERO;
        if (account.getMembership() != null && account.getMembership().getDiscount() != null) {
            int discountPercent = account.getMembership().getDiscount();
            if (discountPercent > 0) {
                memDiscount = rawTotal.multiply(BigDecimal.valueOf(discountPercent)).divide(BigDecimal.valueOf(100));
            }
        }

        // ===== VOUCHER (ĐÃ SỬA: LẤY MÃ CHUẨN XÁC, CHỐNG LỖI MẤT MÃ) =====
        List<Voucher> availableVouchers = new ArrayList<>();

        // 1. Lấy mã user đã lưu từ kho chung
        List<VoucherDetail> myVoucherDetails = voucherDetailRepo.findByAccount_IdAndIsUsedFalse(account.getId());
        if (myVoucherDetails != null) {
            for (VoucherDetail vd : myVoucherDetails) {
                if (vd.getVoucher() != null) availableVouchers.add(vd.getVoucher());
            }
        }

        // 2. Lấy mã tặng riêng cho user
        List<Voucher> myDirectVouchers = voucherRepo.findByAccount_Id(account.getId());
        if (myDirectVouchers != null) {
            availableVouchers.addAll(myDirectVouchers);
        }

        // Lọc các voucher đủ điều kiện
        List<Voucher> savedVouchers = availableVouchers.stream()
                .filter(v -> v != null)
                .filter(v -> v.getActive() == null || v.getActive())
                .filter(v -> v.getMinOrderValue() == null || rawTotal.doubleValue() >= v.getMinOrderValue())
                .distinct()
                .toList();

        BigDecimal voucherDiscount = BigDecimal.ZERO;

        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            Optional<Voucher> vOpt = savedVouchers.stream()
                    .filter(v -> v.getCode().equals(voucherCode.trim()))
                    .findFirst();

            if (vOpt.isPresent()) {
                Voucher v = vOpt.get();

                if (v.getDiscountPercent() != null) {
                    voucherDiscount = rawTotal.multiply(BigDecimal.valueOf(v.getDiscountPercent()).divide(BigDecimal.valueOf(100)));
                } else if (v.getDiscountAmount() != null) {
                    voucherDiscount = BigDecimal.valueOf(v.getDiscountAmount());
                }

                if (Boolean.TRUE.equals(v.getIsFreeShipping())) feeShip = BigDecimal.ZERO;
            }
        }

        // ===== TỔNG HỢP GIẢM GIÁ =====
        BigDecimal totalDiscount = memDiscount.add(voucherDiscount);
        // Đảm bảo không giảm quá số tiền đơn hàng
        if (totalDiscount.compareTo(rawTotal) > 0) {
            totalDiscount = rawTotal;
        }

        BigDecimal finalTotal = rawTotal.subtract(totalDiscount).add(feeShip);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) finalTotal = BigDecimal.ZERO;

        model.addAttribute("cartList", cartList);
        model.addAttribute("addresses", addresses);
        model.addAttribute("selectedAddressId", addressId);
        model.addAttribute("rawTotal", rawTotal);
        
        // Truyền từng loại giảm giá ra view để hiển thị rành mạch
        model.addAttribute("memDiscount", memDiscount); 
        model.addAttribute("voucherDiscount", voucherDiscount);
        
        model.addAttribute("feeShip", feeShip);
        model.addAttribute("finalTotal", finalTotal);
        model.addAttribute("voucherCode", voucherCode);
        model.addAttribute("savedVouchers", savedVouchers); 
        model.addAttribute("user", account);

        return "client/checkout";
    }

    // ================== CONFIRM ORDER ==================
    @PostMapping("/confirm")
    @Transactional
    public String confirmOrder(HttpSession session,
                               @RequestParam(required = false) String voucherCode,
                               @RequestParam("addressId") Long addressId,
                               @RequestParam("paymentMethod") String paymentMethod) {

        Account account = getAccount(session);
        if (account == null) return "redirect:/login";

        List<CartDetail> cartList = cartDetailRepo.findByAccount_Id(account.getId());
        if (cartList.isEmpty()) return "redirect:/cart";

        BigDecimal rawTotal = cartList.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Address selectedAddress = addressRepo.findByIdAndAccount_Id(addressId, account.getId()).orElse(null);
        BigDecimal feeShip = shippingService.calculateFee(selectedAddress, cartList, account);
        
        // ===== TÍNH GIẢM GIÁ THÀNH VIÊN =====
        BigDecimal memDiscount = BigDecimal.ZERO;
        if (account.getMembership() != null && account.getMembership().getDiscount() != null) {
            int discountPercent = account.getMembership().getDiscount();
            if (discountPercent > 0) {
                memDiscount = rawTotal.multiply(BigDecimal.valueOf(discountPercent)).divide(BigDecimal.valueOf(100));
            }
        }

        BigDecimal voucherDiscount = BigDecimal.ZERO;

        // ===== APPLY VOUCHER =====
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            String code = voucherCode.trim();
            Voucher appliedVoucher = null;

            Optional<VoucherDetail> vdOpt = voucherDetailRepo.findValidVoucherForAccount(account.getId(), code);
            if (vdOpt.isPresent() && !Boolean.TRUE.equals(vdOpt.get().getIsUsed())) {
                VoucherDetail vd = vdOpt.get();
                appliedVoucher = vd.getVoucher();
                
                vd.setIsUsed(true);
                vd.setUsedAt(new Date());
                vd.setStatus("USED");
                voucherDetailRepo.save(vd);
                
            } else {
                List<Voucher> directVouchers = voucherRepo.findByAccount_Id(account.getId());
                Optional<Voucher> dirOpt = directVouchers.stream()
                        .filter(v -> v.getCode().equals(code) && (v.getActive() == null || v.getActive()))
                        .findFirst();
                
                if (dirOpt.isPresent()) {
                    appliedVoucher = dirOpt.get();
                    appliedVoucher.setActive(false);
                    voucherRepo.save(appliedVoucher);
                }
            }

            if (appliedVoucher != null) {
                if (appliedVoucher.getDiscountPercent() != null) {
                    voucherDiscount = rawTotal.multiply(BigDecimal.valueOf(appliedVoucher.getDiscountPercent()).divide(BigDecimal.valueOf(100)));
                } else if (appliedVoucher.getDiscountAmount() != null) {
                    voucherDiscount = BigDecimal.valueOf(appliedVoucher.getDiscountAmount());
                }

                if (Boolean.TRUE.equals(appliedVoucher.getIsFreeShipping())) feeShip = BigDecimal.ZERO;
            }
        }

        // TỔNG GIẢM GIÁ
        BigDecimal totalDiscount = memDiscount.add(voucherDiscount);
        if (totalDiscount.compareTo(rawTotal) > 0) {
            totalDiscount = rawTotal;
        }

        BigDecimal finalTotal = rawTotal.subtract(totalDiscount).add(feeShip);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) finalTotal = BigDecimal.ZERO;

        // ===== CREATE ORDER =====
        Orders order = new Orders();
        order.setAccount(account);
        order.setCreatedDate(new Date());
        order.setAddress(selectedAddress);
        if (selectedAddress != null) order.setPhone(selectedAddress.getRecipientPhone());
        order.setTotal(finalTotal);
        order.setFeeship(feeShip);
        order.setMoneyDiscounted(totalDiscount); // Lưu tổng tiền giảm (Gồm Membership + Voucher)
        order.setStatus(0);
        order.setPaymentStatus(false);
        order.setVoucherCode(voucherCode);
        order.setPaymentMethod(paymentMethod);
        ordersRepo.save(order);

        // ===== CREATE ORDER DETAILS + TRỪ KHO =====
        for (CartDetail item : cartList) {
            Products product = productRepo.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            Integer buyQty = item.getQuantity();
            if (product.getQuantity() < buyQty) throw new RuntimeException("Sản phẩm không đủ số lượng: " + product.getName());

            int newQty = product.getQuantity() - buyQty;
            product.setQuantity(newQty);
            product.setSold((product.getSold() == null ? 0 : product.getSold()) + buyQty);
            if (newQty <= 0) product.setAvailable(false);
            productRepo.saveAndFlush(product);

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(buyQty);
            detail.setPrice(product.getPrice());
            orderDetailRepo.save(detail);
        }

        // ===== XÓA CART & VNPAY =====
        cartDetailRepo.deleteAll(cartList);

        if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            long amount = finalTotal.longValue() * 100;
            String paymentUrl = vnPayService.createOrder((int) amount, String.valueOf(order.getId()), VNPayConfig.vnp_ReturnUrl);
            return "redirect:" + paymentUrl;
        }

        return "redirect:/orders";
    }

    // ================== VNPAY RETURN ==================
    @GetMapping("/vnpay-return")
    @Transactional
    public String vnpayReturn(HttpServletRequest request) {
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String orderIdStr = request.getParameter("vnp_TxnRef");

        if (orderIdStr != null && !orderIdStr.isEmpty()) {
            try {
                Integer orderId = Integer.parseInt(orderIdStr);
                Orders order = ordersRepo.findById(orderId).orElse(null);

                if (order != null) {
                    order.setPaymentStatus("00".equals(vnp_ResponseCode));
                    ordersRepo.save(order);
                    return "redirect:/orders?payment=" + ("00".equals(vnp_ResponseCode) ? "success" : "failed");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "redirect:/orders";
    }

    // ================== GET ACCOUNT ==================
    private Account getAccount(HttpSession session) {
        Account account = (Account) session.getAttribute("account");
        return account != null ? account : (Account) session.getAttribute("user");
    }
}