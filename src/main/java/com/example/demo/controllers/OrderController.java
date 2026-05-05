package com.example.demo.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Account;
import com.example.demo.model.Comment;
import com.example.demo.model.OrderDetail; // Đã thêm import này để xử lý hoàn kho
import com.example.demo.model.Orders;
import com.example.demo.model.Products; // Đã thêm import này để xử lý hoàn kho
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.OrdersRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.OrderService;
import com.example.demo.service.VNPayService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class OrderController {

    @Autowired private HttpSession session;
    @Autowired private OrdersRepository orderRepo;
    @Autowired private OrderService orderService;
    @Autowired private CommentRepository commentRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private VNPayService vnPayService;

    @GetMapping("/orders")
    public String viewOrders(Model model, @RequestParam(name = "sort", defaultValue = "newest") String sort,
                             @RequestParam(name = "payment", required = false) String payment) {
        Account account = getAccount();
        if (account == null) {
			return "redirect:/login";
		}

        List<Orders> orders = "oldest".equals(sort)
            ? orderRepo.findByAccount_IdOrderByCreatedDateAsc(account.getId())
            : orderRepo.findByAccount_IdOrderByCreatedDateDesc(account.getId());

        List<String> reviewedKeys = orders.stream()
            .flatMap(o -> commentRepo.findByAccount_IdAndOrderId(account.getId(), o.getId()).stream())
            .map(c -> c.getOrderId() + "_" + c.getProduct().getId())
            .collect(Collectors.toList());

        model.addAttribute("orders", orders);
        model.addAttribute("reviewedKeys", reviewedKeys);
        model.addAttribute("user", account);
        return "client/orders";
    }

    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request, Model model) {
        int status = vnPayService.orderReturn(request);
        if (status == 1) {
            try {
                String orderIdStr = request.getParameter("vnp_OrderInfo").replaceAll("[^0-9]", "");
                Orders order = orderRepo.findById(Integer.parseInt(orderIdStr)).orElse(null);
                if (order != null) {
                    order.setPaymentStatus(true);
                    orderRepo.save(order);
                    
                    // Truyền thông tin sang trang order-success
                    model.addAttribute("orderId", order.getId());
                    model.addAttribute("total", order.getTotal());
                }
            } catch (Exception e) { 
                e.printStackTrace(); 
            }
            // Chuyển hướng đến trang thông báo thành công
            return "client/order-success"; 
        }
        // Nếu thất bại vẫn trả về trang orders để hiện Popup SweetAlert lỗi
        return "redirect:/orders?payment=failed";
    }

    @PostMapping("/orders/submit-review")
    public String submitReview(@RequestParam("productId") Integer productId, @RequestParam("orderId") Integer orderId,
                               @RequestParam("rating") Integer rating, @RequestParam("content") String content) {
        Account account = getAccount();
        if (account == null) {
			return "redirect:/login";
		}
        if (!commentRepo.existsByAccount_IdAndProduct_IdAndOrderId(account.getId(), productId, orderId)) {
            Comment comment = new Comment();
            comment.setAccount(account);
            comment.setOrderId(orderId);
            comment.setRating(rating);
            comment.setContent(content);
            comment.setCreatedAt(LocalDateTime.now());
            productRepo.findById(productId).ifPresent(comment::setProduct);
            commentRepo.save(comment);
        }
        return "redirect:/orders?reviewSuccess=true";
    }

    @PostMapping("/orders/confirm/{id}")
    public String confirmOrder(@PathVariable("id") Integer orderId) {
        Account account = getAccount();
        if (account == null) {
			return "redirect:/login";
		}
        orderRepo.findById(orderId).ifPresent(order -> {
            if (order.getAccount().getId().equals(account.getId()) && order.getStatus() == 3) {
                orderService.completeOrder(order);
            }
        });
        return "redirect:/orders";
    }

    @PostMapping("/orders/cancel/{id}")
    public String cancelOrder(@PathVariable("id") Integer orderId) {
        Account account = getAccount();
        if (account == null) {
			return "redirect:/login";
		}
        orderRepo.findById(orderId).ifPresent(order -> {
            if (order.getAccount().getId().equals(account.getId()) && (order.getStatus() == 0 || order.getStatus() == 1)) {
                
                // --- ĐÃ THÊM LOGIC HOÀN KHO KHI USER HỦY ĐƠN ---
                for (OrderDetail detail : order.getOrderDetails()) {
                    Products product = detail.getProduct();
                    if (product != null) {
                        int currentStock = (product.getQuantity() != null) ? product.getQuantity() : 0;
                        product.setQuantity(currentStock + detail.getQuantity());
                        
                        if (product.getQuantity() > 0) {
                            product.setAvailable(true);
                        }
                        productRepo.save(product); // Lưu kho
                    }
                }
                // ----------------------------------------------
                
                order.setStatus(5);
                orderRepo.save(order);
            }
        });
        return "redirect:/orders";
    }

    private Account getAccount() {
        Account acc = (Account) session.getAttribute("account");
        return (acc != null) ? acc : (Account) session.getAttribute("user");
    }
}