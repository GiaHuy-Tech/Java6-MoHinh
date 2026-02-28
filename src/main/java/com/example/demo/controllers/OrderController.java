package com.example.demo.controllers;

import java.util.List; 
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Account;
import com.example.demo.model.Orders;
import com.example.demo.model.OrderDetail; // MỚI THÊM: Để lấy chi tiết đơn
import com.example.demo.model.Products;     // MỚI THÊM: Để lấy thông tin sản phẩm
import com.example.demo.repository.OrdersRepository;
import com.example.demo.repository.ProductRepository; // MỚI THÊM: Để cập nhật kho

import jakarta.servlet.http.HttpSession;

@Controller
public class OrderController {

    @Autowired
    private HttpSession session;

    @Autowired
    private OrdersRepository orderRepo;
    
    @Autowired
    private ProductRepository productRepo; // MỚI THÊM: Inject ProductRepository

    /**
     * Hiển thị trang "Đơn hàng của tôi" có chức năng Sắp xếp
     */
    @GetMapping("/orders")
    public String viewOrders(Model model,
                             @RequestParam(name = "sort", defaultValue = "newest") String sort) {

        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        List<Orders> orders;

        // Xử lý logic sắp xếp
        if ("oldest".equals(sort)) {
            orders = orderRepo.findByAccountId_IdOrderByCreatedDateAsc(account.getId());
        } else {
            orders = orderRepo.findByAccountId_IdOrderByCreatedDateDesc(account.getId());
        }

        model.addAttribute("orders", orders);
        model.addAttribute("sort", sort);

        return "client/orders";
    }

    /**
     * Xem chi tiết đơn hàng
     */
    @GetMapping("/orders/detail/{id}")
    public String viewOrderDetail(@PathVariable("id") Integer orderId, Model model) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        Optional<Orders> orderOpt = orderRepo.findById(orderId);

        if (orderOpt.isPresent()) {
            Orders order = orderOpt.get();
            if (order.getAccountId().getId().equals(account.getId())) {
                model.addAttribute("order", order);
                model.addAttribute("orderDetails", order.getOrderDetails());
                return "client/order-detail";
            }
        }

        return "redirect:/orders";
    }

    /**
     * MỚI THÊM: Xử lý Khách xác nhận đã nhận hàng (Hoàn thành đơn) -> TRỪ KHO
     */
    @PostMapping("/orders/confirm/{id}")
    public String confirmOrder(@PathVariable("id") Integer orderId) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        Optional<Orders> optionalOrder = orderRepo.findById(orderId);
        if (optionalOrder.isPresent()) {
            Orders order = optionalOrder.get();

            // Kiểm tra: Phải là đơn của user đó VÀ trạng thái đang là "Đang giao" (ví dụ status = 2) 
            // hoặc chưa hoàn thành. Tránh trường hợp trừ kho 2 lần.
            // Giả sử: 3 là trạng thái "Hoàn thành"
            if (order.getAccountId().getId().equals(account.getId()) && order.getStatus() != 3) {

                // --- LOGIC TRỪ KHO BẮT ĐẦU TẠI ĐÂY ---
                for (OrderDetail detail : order.getOrderDetails()) {
                    Product product = detail.getProduct();
                    
                    // Tính số lượng mới = Tồn kho - Số lượng mua
                    int newQuantity = product.getQuantity() - detail.getQuantity();
                    
                    // Cập nhật số lượng
                    product.setQuantity(newQuantity);
                    
                    // Nếu hết hàng thì ẩn sản phẩm luôn (tuỳ chọn)
                    if (newQuantity <= 0) {
                        product.setAvailable(false);
                    }
                    
                    // Lưu lại Product
                    productRepo.save(product);
                }
                // --- KẾT THÚC LOGIC TRỪ KHO ---

                order.setStatus(3); // 3 = Hoàn thành (Đã nhận hàng)
                orderRepo.save(order);
            }
        }
        return "redirect:/orders";
    }

    /**
     * Xử lý hủy đơn
     */
    @PostMapping("/orders/cancel/{id}")
    public String cancelOrder(@PathVariable("id") Integer orderId) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        Optional<Orders> optionalOrder = orderRepo.findById(orderId);
        if (optionalOrder.isPresent()) {
            Orders order = optionalOrder.get();
            // Chỉ cho phép hủy đơn chờ xử lý (0) hoặc đã xác nhận (1)
            if (order.getAccountId().getId().equals(account.getId()) &&
                (order.getStatus() == 0 || order.getStatus() == 1)) {

                order.setStatus(4); // 4 = Đã hủy
                orderRepo.save(order);
            }
        }

        return "redirect:/orders";
    }
}