package com.example.demo.controllers;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Account;
import com.example.demo.model.Orders;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.OrdersRepository;
import com.example.demo.service.MailService;

@Controller
@RequestMapping("/orders-mana")
public class OrdersManaController {

    @Autowired
    private OrdersRepository ordersRepo;

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private MailService mailService;

    // --- CẤU HÌNH DANH SÁCH TỈNH THÀNH (Kho Cần Thơ) ---
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

    // --- HÀM TÍNH SHIP (Tái sử dụng logic) ---
    private int calculateShippingFee(String address, int subTotal) {
        if (subTotal >= 1000000) {
			return 0;
		}
        if (address == null || address.isEmpty()) {
			return 50000;
		}

        String normAddress = unAccent(address.toLowerCase());

        if (normAddress.contains("can tho")) {
			return 20000; // Nội thành
		}

        for (String p : SOUTH_PROVINCES) {
			if (normAddress.contains(p)) {
				return 30000;
			}
		}
        for (String p : CENTRAL_PROVINCES) {
			if (normAddress.contains(p)) {
				return 40000;
			}
		}
        for (String p : NORTH_PROVINCES) {
			if (normAddress.contains(p)) {
				return 50000;
			}
		}

        return 45000;
    }

    public static String unAccent(String s) {
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replaceAll("đ", "d");
    }

    // --- ENDPOINTS ---

    // ✅ Danh sách đơn hàng
    @GetMapping
    public String list(Model model) {
        model.addAttribute("ordersList", ordersRepo.findAll());
        model.addAttribute("order", new Orders());
        model.addAttribute("accounts", accountRepo.findAll());
        return "admin/orders-mana";
    }

    // ✅ Thêm mới đơn hàng (Admin tạo hộ khách)
    @PostMapping("/add")
    public String add(
            @RequestParam("accountId") Integer accountId,
            @ModelAttribute("order") Orders order) {

        Account acc = accountRepo.findById(accountId).orElse(null);
        order.setAccountId(acc);
        order.setCreatedDate(new Date());

        // TÍNH PHÍ SHIP CHO ĐƠN MỚI
        int currentTotal = order.getTotal();
        int fee = calculateShippingFee(order.getAddress(), currentTotal);

        order.setFeeship(fee);
        order.setTotal(currentTotal + fee);

        ordersRepo.save(order);
        return "redirect:/orders-mana";
    }

    // ✅ Form sửa đơn hàng
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Integer id, Model model) {
        Orders order = ordersRepo.findById(id).orElse(null);
        if (order == null) {
			return "redirect:/orders-mana";
		}

        model.addAttribute("order", order);
        model.addAttribute("ordersList", ordersRepo.findAll());
        model.addAttribute("accounts", accountRepo.findAll());
        return "admin/order-edit";
    }

    // ✅ Cập nhật TRẠNG THÁI ĐƠN HÀNG (Status)
    @PostMapping("/updateStatus")
    public String updateStatus(@RequestParam("id") Integer id,
                               @RequestParam("status") int status) {

        Orders order = ordersRepo.findById(id).orElse(null);
        if (order != null) {
            order.setStatus(status);

            // Tính lại phí ship nếu cần (khi cập nhật trạng thái có thể do admin sửa địa chỉ trước đó)
            int subTotal = order.getOrderDetails().stream()
                    .mapToInt(d -> d.getPrice() * d.getQuantity())
                    .sum();

            int newFee = calculateShippingFee(order.getAddress(), subTotal);

            if (newFee != order.getFeeship()) {
                order.setFeeship(newFee);
                order.setTotal(subTotal + newFee);
            }

            ordersRepo.save(order);

            // Gửi mail thông báo
            Account acc = order.getAccountId();
            if (acc != null && acc.getEmail() != null) {
                String subject = "Cập nhật trạng thái đơn hàng #" + order.getId();
                String body = "Xin chào " + acc.getFullName() + ",\n\n"
                        + "Trạng thái đơn hàng của bạn vừa được cập nhật: "
                        + getStatusText(status)
                        + "\n\nCảm ơn bạn đã mua hàng tại Mom Physic High End Model!";
                mailService.sendStatusMail(acc.getEmail(), subject, body);
            }
        }
        return "redirect:/orders-mana";
    }

    // ✅ Cập nhật TRẠNG THÁI THANH TOÁN (PaymentStatus) - MỚI THÊM
    @PostMapping("/updatePaymentStatus")
    public String updatePaymentStatus(@RequestParam("id") Integer id,
                                      @RequestParam("paymentStatus") boolean paymentStatus) {

        Orders order = ordersRepo.findById(id).orElse(null);
        if (order != null) {
            order.setPaymentStatus(paymentStatus);
            ordersRepo.save(order);
        }
        // Quay lại trang chi tiết đơn hàng để Admin thấy ngay kết quả
        return "redirect:/orders-mana/detail/" + id;
    }

    private String getStatusText(int status) {
        switch (status) {
            case 0: return "Chờ xử lý";
            case 1: return "Đã xác nhận";
            case 2: return "Đang giao hàng";
            case 3: return "Hoàn tất";
            case 4: return "Đã hủy";
            default: return "Không xác định";
        }
    }

    // ✅ Xem chi tiết đơn hàng (Admin)
    @GetMapping("/detail/{id}")
    public String orderDetail(@PathVariable("id") Integer id, Model model) {
        Orders order = ordersRepo.findById(id).orElse(null);
        if (order == null) {
			return "redirect:/orders-mana";
		}

        model.addAttribute("order", order);
        model.addAttribute("orderDetails", order.getOrderDetails());
        return "admin/order-detail";
    }

    // Route cũ (giữ để tương thích ngược nếu cần)
    @GetMapping("/cart/{orderId}")
    public String cartDetail(@PathVariable("orderId") Integer orderId, Model model) {
        return orderDetail(orderId, model);
    }
}