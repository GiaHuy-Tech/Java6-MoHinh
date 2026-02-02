package com.example.demo.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    // ✅ Thêm sản phẩm vào giỏ hàng
    @GetMapping("/add/{productId}")
    public String addToCart(@PathVariable Integer productId,
                            @RequestParam(name = "quantity", defaultValue = "1") Integer quantity,
                            @RequestParam(name = "customPrice", required = false) Double customPrice) {

        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        // Lấy sản phẩm từ DB
        Products product = productRepo.findById(productId).orElse(null);
        if (product == null) {
            return "redirect:/";
        }

        // Tìm hoặc tạo mới giỏ hàng (Cart) cho user
        Cart cart = cartRepo.findByAccount(account).orElse(null);
        if (cart == null) {
            cart = new Cart();
            cart.setAccount(account);
            cartRepo.save(cart);
        }

        // QUAN TRỌNG: Quyết định giá bán
        // Nếu trang Detail gửi giá Custom (đã check logic bên đó) thì lấy, không thì lấy giá gốc
        double priceToSave = (customPrice != null) ? customPrice : product.getPrice();

        // Kiểm tra sản phẩm đã có trong giỏ chưa
        CartDetail existing = cartDetailRepo.findByCartAndProduct(cart, product).orElse(null);

        if (existing != null) {
            // Nếu đã có: Cộng dồn số lượng
            existing.setQuantity(existing.getQuantity() + quantity);

            // Cập nhật giá mới nhất (ép kiểu int)
            existing.setPrice((int) priceToSave);

            cartDetailRepo.save(existing);
        } else {
            // Nếu chưa có: Tạo mới chi tiết giỏ hàng
            CartDetail cd = new CartDetail();
            cd.setCart(cart);
            cd.setProduct(product);

            // Lưu giá (ép kiểu int)
            cd.setPrice((int) priceToSave);

            cd.setQuantity(quantity);
            cartDetailRepo.save(cd);
        }

        return "redirect:/cart";
    }
}