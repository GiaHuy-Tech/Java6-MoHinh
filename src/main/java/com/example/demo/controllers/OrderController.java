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
import com.example.demo.model.OrderDetail;
import com.example.demo.model.Products;
import com.example.demo.repository.OrdersRepository;
import com.example.demo.repository.ProductRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class OrderController {

    @Autowired
    private HttpSession session;

    @Autowired
    private OrdersRepository orderRepo;
    
    @Autowired
    private ProductRepository productRepo;

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

        // SỬA: Gọi đúng tên hàm trong Repository đã khai báo
        if ("oldest".equals(sort)) {
            orders = orderRepo.findByAccount_IdOrderByCreatedDateAsc(account.getId());
        } else {
            orders = orderRepo.findByAccount_IdOrderByCreatedDateDesc(account.getId());
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
            // SỬA: getAccountId() -> getAccount().getId() vì quan hệ là đối tượng Account
            if (order.getAccount().getId().equals(account.getId())) {
                model.addAttribute("order", order);
                model.addAttribute("orderDetails", order.getOrderDetails());
                return "client/order-detail";
            }
        }

        return "redirect:/orders";
    }

    /**
     * Xử lý Khách xác nhận đã nhận hàng (Hoàn thành đơn) -> TRỪ KHO
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

            // Status: 0=Chờ, 1=Xác nhận, 2=Đang giao, 3=Hoàn thành, 4=Hủy
            // Chỉ cho phép xác nhận khi đơn chưa hoàn thành (status != 3) và đúng chủ sở hữu
            if (order.getAccount().getId().equals(account.getId()) && order.getStatus() != 3 && order.getStatus() != 4) {

                // --- LOGIC TRỪ KHO ---
                List<OrderDetail> details = order.getOrderDetails();
                
                if (details != null) {
                    for (OrderDetail detail : details) {
                        Products product = detail.getProduct();
                        
                        // Kiểm tra null để tránh lỗi NullPointerException
                        if (product != null) {
                            int currentStock = product.getQuantity();
                            int quantitySold = detail.getQuantity();
                            
                            // Tính tồn kho mới
                            int newQuantity = currentStock - quantitySold;
                            
                            // Đảm bảo không âm
                            if (newQuantity < 0) newQuantity = 0;

                            product.setQuantity(newQuantity);
                            
                            // Nếu hết hàng thì ẩn sản phẩm
                            if (newQuantity <= 0) {
                                product.setAvailable(false);
                            }
                            
                            productRepo.save(product);
                        }
                    }
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
            
            // SỬA: getAccount().getId()
            // Chỉ cho phép hủy đơn chờ xử lý (0) hoặc đã xác nhận (1)
            if (order.getAccount().getId().equals(account.getId()) &&
                (order.getStatus() == 0 || order.getStatus() == 1)) {

                order.setStatus(4); // 4 = Đã hủy
                orderRepo.save(order);
            }
        }

        return "redirect:/orders";
    }
}