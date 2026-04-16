package com.example.demo.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
import com.example.demo.model.Orders;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.OrdersRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.OrderService;

import jakarta.servlet.http.HttpSession;

@Controller
public class OrderController {

    @Autowired private HttpSession session;
    @Autowired private OrdersRepository orderRepo;
    @Autowired private OrderService orderService;
    @Autowired private CommentRepository commentRepo;
    @Autowired private ProductRepository productRepo;

    @GetMapping("/orders")
    public String viewOrders(Model model,
                             @RequestParam(name = "sort", defaultValue = "newest") String sort,
                             @RequestParam(name = "reviewSuccess", required = false) String reviewSuccess) {
        Account account = getAccount();
        if (account == null) {
			return "redirect:/login";
		}

        List<Orders> orders;
        if ("oldest".equals(sort)) {
            orders = orderRepo.findByAccount_IdOrderByCreatedDateAsc(account.getId());
        } else {
            orders = orderRepo.findByAccount_IdOrderByCreatedDateDesc(account.getId());
        }

        // Tạo một danh sách các "Mã định danh" (OrderId_ProductId) đã được đánh giá
        // Việc này giúp giao diện biết nút nào nên ẩn/hiện
        List<String> reviewedKeys = orders.stream()
            .flatMap(o -> commentRepo.findByAccount_IdAndOrderId(account.getId(), o.getId()).stream())
            .map(c -> c.getOrderId() + "_" + c.getProduct().getId())
            .collect(Collectors.toList());

        model.addAttribute("orders", orders);
        model.addAttribute("reviewedKeys", reviewedKeys); // Gửi sang HTML
        model.addAttribute("sort", sort);
        model.addAttribute("user", account);
        model.addAttribute("reviewSuccess", reviewSuccess);

        return "client/orders";
    }

    @PostMapping("/orders/submit-review")
    public String submitReview(@RequestParam("productId") Integer productId,
                               @RequestParam("orderId") Integer orderId, // Thêm orderId
                               @RequestParam("rating") Integer rating,
                               @RequestParam("content") String content) {
        Account account = getAccount();
        if (account == null) {
			return "redirect:/login";
		}

        // Kiểm tra xem đã đánh giá cho SẢN PHẨM này trong ĐƠN HÀNG này chưa
        boolean exists = commentRepo.existsByAccount_IdAndProduct_IdAndOrderId(account.getId(), productId, orderId);

        if (!exists) {
            Comment comment = new Comment();
            comment.setAccount(account);
            comment.setOrderId(orderId); // Lưu thông tin đơn hàng
            comment.setRating(rating);
            comment.setContent(content);
            comment.setCreatedAt(LocalDateTime.now());
            productRepo.findById(productId).ifPresent(comment::setProduct);
            commentRepo.save(comment);
        }

        return "redirect:/orders?reviewSuccess=true";
    }

    // Các hàm confirmOrder, cancelOrder và getAccount giữ nguyên như cũ...
    @PostMapping("/orders/confirm/{id}")
    public String confirmOrder(@PathVariable("id") Integer orderId) {
        Account account = getAccount();
        if (account == null) {
			return "redirect:/login";
		}
        Optional<Orders> optionalOrder = orderRepo.findById(orderId);
        if (optionalOrder.isPresent()) {
            Orders order = optionalOrder.get();
            if (order.getAccount().getId().equals(account.getId()) && order.getStatus() == 3) {
                orderService.completeOrder(order);
            }
        }
        return "redirect:/orders";
    }

    @PostMapping("/orders/cancel/{id}")
    public String cancelOrder(@PathVariable("id") Integer orderId) {
        Account account = getAccount();
        if (account == null) {
			return "redirect:/login";
		}
        Optional<Orders> optionalOrder = orderRepo.findById(orderId);
        if (optionalOrder.isPresent()) {
            Orders order = optionalOrder.get();
            if (order.getAccount().getId().equals(account.getId()) && (order.getStatus() == 0 || order.getStatus() == 1)) {
                order.setStatus(5);
                orderRepo.save(order);
            }
        }
        return "redirect:/orders";
    }

    private Account getAccount() {
        Account acc = (Account) session.getAttribute("account");
        return (acc != null) ? acc : (Account) session.getAttribute("user");
    }
}