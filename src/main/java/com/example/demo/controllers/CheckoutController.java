package com.example.demo.controllers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

import jakarta.servlet.http.HttpServletRequest; // Bổ sung import này để dùng request
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
            selectedAddress = addressRepo
                    .findByIdAndAccount_Id(addressId, account.getId())
                    .orElse(null);
        }

        if (selectedAddress == null) {
            selectedAddress = addresses.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                    .findFirst()
                    .orElse(null);
        }

        // ===== SHIPPING =====
        BigDecimal feeShip = shippingService.calculateFee(selectedAddress, cartList);

        // ===== VOUCHER =====
        List<VoucherDetail> myVoucherDetails =
                voucherDetailRepo.findByAccount_IdAndIsUsedFalse(account.getId());

        List<Voucher> publicVouchers = voucherRepo.findByAccountIsNull();

        List<Voucher> availableVouchers = new ArrayList<>();
        myVoucherDetails.forEach(vd -> availableVouchers.add(vd.getVoucher()));
        availableVouchers.addAll(publicVouchers);

        LocalDateTime now = LocalDateTime.now();

        List<Voucher> savedVouchers = availableVouchers.stream()
                .filter(v -> v != null && Boolean.TRUE.equals(v.getActive()))
                .filter(v -> v.getExpiredAt() == null || v.getExpiredAt().isAfter(now))
                .filter(v -> v.getMinOrderValue() == null
                        || rawTotal.doubleValue() >= v.getMinOrderValue())
                .distinct()
                .toList();

        BigDecimal discount = BigDecimal.ZERO;

        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            Optional<Voucher> vOpt = savedVouchers.stream()
                    .filter(v -> v.getCode().equals(voucherCode.trim()))
                    .findFirst();

            if (vOpt.isPresent()) {
                Voucher v = vOpt.get();

                if (v.getDiscountPercent() != null) {
                    discount = rawTotal.multiply(
                            BigDecimal.valueOf(v.getDiscountPercent())
                                    .divide(BigDecimal.valueOf(100)));
                } else if (v.getDiscountAmount() != null) {
                    discount = BigDecimal.valueOf(v.getDiscountAmount());
                }

                if (discount.compareTo(rawTotal) > 0) {
					discount = rawTotal;
				}

                if (Boolean.TRUE.equals(v.getIsFreeShipping())) {
                    feeShip = BigDecimal.ZERO;
                }
            }
        }

        BigDecimal finalTotal = rawTotal.subtract(discount).add(feeShip);

        model.addAttribute("cartList", cartList);
        model.addAttribute("addresses", addresses);
        model.addAttribute("selectedAddressId", addressId);
        model.addAttribute("rawTotal", rawTotal);
        model.addAttribute("discount", discount);
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
        Address selectedAddress = addressRepo
                .findByIdAndAccount_Id(addressId, account.getId())
                .orElse(null);

        // ===== SHIPPING =====
        BigDecimal feeShip = shippingService.calculateFee(selectedAddress, cartList);

        BigDecimal discount = BigDecimal.ZERO;

        // ===== APPLY VOUCHER =====
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            Optional<Voucher> vOpt = voucherRepo.findAll().stream()
                    .filter(v -> v.getCode().equals(voucherCode.trim())
                            && Boolean.TRUE.equals(v.getActive()))
                    .findFirst();

            if (vOpt.isPresent()) {
                Voucher v = vOpt.get();

                if (v.getDiscountPercent() != null) {
                    discount = rawTotal.multiply(
                            BigDecimal.valueOf(v.getDiscountPercent())
                                    .divide(BigDecimal.valueOf(100)));
                } else if (v.getDiscountAmount() != null) {
                    discount = BigDecimal.valueOf(v.getDiscountAmount());
                }

                if (discount.compareTo(rawTotal) > 0) {
					discount = rawTotal;
				}

                if (Boolean.TRUE.equals(v.getIsFreeShipping())) {
                    feeShip = BigDecimal.ZERO;
                }

                voucherDetailRepo.findValidVoucherForAccount(account.getId(), voucherCode.trim())
                        .ifPresent(vd -> {
                            vd.setIsUsed(true);
                            vd.setUsedAt(new Date());
                            vd.setStatus("USED");
                            voucherDetailRepo.save(vd);
                        });
            }
        }

        BigDecimal finalTotal = rawTotal.subtract(discount).add(feeShip);

        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        // ===== CREATE ORDER =====
        Orders order = new Orders();
        order.setAccount(account);
        order.setCreatedDate(new Date());
        order.setAddress(selectedAddress);

        if (selectedAddress != null) {
            order.setPhone(selectedAddress.getRecipientPhone());
        }

        order.setTotal(finalTotal);
        order.setFeeship(feeShip);
        order.setMoneyDiscounted(discount);
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

            // CHECK tồn kho
            if (product.getQuantity() < buyQty) {
                throw new RuntimeException(
                        "Sản phẩm " + product.getName() + " không đủ số lượng trong kho");
            }

            // TRỪ KHO
            int newQty = product.getQuantity() - buyQty;
            product.setQuantity(newQty);

            // TĂNG SOLD
            Integer sold = product.getSold() == null ? 0 : product.getSold();
            product.setSold(sold + buyQty);

            // HẾT HÀNG
            if (newQty <= 0) {
                product.setAvailable(false);
            }

            productRepo.saveAndFlush(product);

            // ORDER DETAIL
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(buyQty);
            detail.setPrice(product.getPrice());

            orderDetailRepo.save(detail);
        }

        // ===== XÓA CART =====
        cartDetailRepo.deleteAll(cartList);

        // ===== VNPAY =====
        if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            long amount = finalTotal.longValue() * 100;

            String paymentUrl = vnPayService.createOrder(
                    (int) amount,
                    String.valueOf(order.getId()),
                    VNPayConfig.vnp_ReturnUrl
            );

            return "redirect:" + paymentUrl;
        }

        return "redirect:/orders";
    }

    // ================== VNPAY RETURN (Đã thêm để fix lỗi 404 & Type Mismatch) ==================
    @GetMapping("/vnpay-return")
    @Transactional
    public String vnpayReturn(HttpServletRequest request) {

        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String orderIdStr = request.getParameter("vnp_TxnRef");

        if (orderIdStr != null && !orderIdStr.isEmpty()) {
            try {
                // Đã chuyển thành Integer để khớp với id của model Orders
                Integer orderId = Integer.parseInt(orderIdStr);
                Orders order = ordersRepo.findById(orderId).orElse(null);

                if (order != null) {
                    if ("00".equals(vnp_ResponseCode)) {
                        order.setPaymentStatus(true);
                        ordersRepo.save(order);
                        return "redirect:/orders?payment=success";
                    } else {
                        order.setPaymentStatus(false);
                        ordersRepo.save(order);
                        return "redirect:/orders?payment=failed";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return "redirect:/";
    }

    // ================== GET ACCOUNT ==================
    private Account getAccount(HttpSession session) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            account = (Account) session.getAttribute("user");
        }
        return account;
    }
}