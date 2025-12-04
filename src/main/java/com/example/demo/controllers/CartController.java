package com.example.demo.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Account;
import com.example.demo.model.Cart;
import com.example.demo.model.CartDetail;
import com.example.demo.model.Products;
import com.example.demo.repository.CartDetailRepository;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.ProductRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartDetailRepository cartDetailRepo;

    @Autowired
    private CartRepository cartRepo;

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private HttpSession session;

    // ✅ Hiển thị giỏ hàng
    @GetMapping
    public String viewCart(Model model) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        List<CartDetail> cartDetails = cartDetailRepo.findByCart_Account_Id(account.getId());
        double total = cartDetails.stream()
                .mapToDouble(cd -> cd.getPrice() * cd.getQuantity())
                .sum();

        model.addAttribute("cartDetails", cartDetails);
        model.addAttribute("total", total);
        return "client/cart";
    }

    // ✅ Xóa sản phẩm khỏi giỏ hàng
    @GetMapping("/delete/{id}")
    public String deleteCartItem(@PathVariable Integer id) {
        cartDetailRepo.deleteById(id);
        return "redirect:/cart";
    }

    // ✅ Thêm sản phẩm vào giỏ hàng và chuyển sang trang giỏ hàng
    @GetMapping("/add/{productId}")
    public String addToCart(@PathVariable Integer productId,
                            @RequestParam(name = "quantity", defaultValue = "1") Integer quantity) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        // Lấy sản phẩm
        Products product = productRepo.findById(productId).orElse(null);
        if (product == null) {
            return "redirect:/";
        }

        // Tìm hoặc tạo mới giỏ hàng
        Cart cart = cartRepo.findByAccount(account).orElse(null);
        if (cart == null) {
            cart = new Cart();
            cart.setAccount(account);
            cartRepo.save(cart);
        }

        // Kiểm tra sản phẩm đã có chưa
        CartDetail existing = cartDetailRepo.findByCartAndProduct(cart, product).orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity); // ✅ cộng dồn đúng số lượng
            cartDetailRepo.save(existing);
        } else {
            CartDetail cd = new CartDetail();
            cd.setCart(cart);
            cd.setProduct(product);
            cd.setPrice(product.getPrice());
            cd.setQuantity(quantity); // ✅ lưu số lượng người nhập
            cartDetailRepo.save(cd);
        }

        return "redirect:/cart";
    


    }
}
