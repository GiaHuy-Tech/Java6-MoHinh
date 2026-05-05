package com.example.demo.controllers;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Account;
import com.example.demo.model.CartDetail;
import com.example.demo.model.Products;
import com.example.demo.repository.CartDetailRepository;
import com.example.demo.repository.ProductRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartDetailRepository cartRepo;

    @Autowired
    private ProductRepository productRepo;

    // ================= 1. TRANG GIỎ HÀNG =================
    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        Account account = getAccount(session);
        if (account == null) {
			return "redirect:/login";
		}

        List<CartDetail> cartList = cartRepo.findCartWithProduct(account.getId());
        BigDecimal total = BigDecimal.ZERO;

        for (CartDetail item : cartList) {
            BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
            BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
            total = total.add(price.multiply(qty));
        }

        model.addAttribute("cartDetails", cartList);
        model.addAttribute("total", total);
        model.addAttribute("cart", cartList);
        model.addAttribute("cartSize", cartList.size());

        return "client/cart";
    }

    // ================= 2. THÊM SẢN PHẨM (CÓ CHECK KHO) =================
    @PostMapping("/add/{productId}")
    @ResponseBody
    public String addToCart(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "1") int quantity,
            HttpSession session) {

        Account account = getAccount(session);
        if (account == null) {
			return "unauthorized";
		}

        Products product = productRepo.findById(productId).orElse(null);
        if (product == null) {
			return "error";
		}

        // Kiểm tra nếu sản phẩm không còn kinh doanh hoặc hết sạch hàng ngay từ đầu
        if (!product.isAvailable() || product.getQuantity() <= 0) {
            return "out_of_stock";
        }

        CartDetail cartItem = cartRepo.findByAccountAndProduct(account, product).orElse(null);
        int currentQtyInCart = (cartItem != null) ? cartItem.getQuantity() : 0;
        int maxAvailable = product.getQuantity();

        // CHẶN: Nếu số lượng mới vượt quá kho
        if (currentQtyInCart + quantity > maxAvailable) {
            return "limit:" + maxAvailable; // Trả về "limit:10" để frontend biết giới hạn
        }

        try {
            if (cartItem != null) {
                cartItem.setQuantity(currentQtyInCart + quantity);
                cartRepo.save(cartItem);
            } else {
                cartItem = new CartDetail();
                cartItem.setAccount(account);
                cartItem.setProduct(product);
                cartItem.setQuantity(quantity);
                cartItem.setCreateDate(new Date());
                cartItem.setPrice(product.getPrice());
                cartRepo.save(cartItem);
            }
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    // ================= 3. MINI CART API =================
    @GetMapping("/api/mini-cart")
    @ResponseBody
    public ResponseEntity<List<CartDetail>> getMiniCartData(HttpSession session) {
        Account account = getAccount(session);
        if (account == null) {
			return ResponseEntity.status(401).build();
		}
        List<CartDetail> cartList = cartRepo.findCartWithProduct(account.getId());
        return ResponseEntity.ok(cartList);
    }

    // ================= TĂNG SỐ LƯỢNG (CHECK KHO) =================
    @GetMapping("/plus/{id}")
    public String increase(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        Account account = getAccount(session);
        if (account == null) {
			return "redirect:/login";
		}

        CartDetail item = cartRepo.findById(id).orElse(null);

        if (item != null && item.getAccount().getId().equals(account.getId())) {
            // KIỂM TRA TỒN KHO TRƯỚC KHI TĂNG
            if (item.getQuantity() < item.getProduct().getQuantity()) {
                item.setQuantity(item.getQuantity() + 1);
                cartRepo.save(item);
            } else {
                // Gửi thông báo lỗi ra trang giỏ hàng
                ra.addFlashAttribute("error", "Số lượng sản phẩm trong kho đã đạt giới hạn tối đa!");
            }
        }

        return "redirect:/cart";
    }

    // ================= GIẢM SỐ LƯỢNG =================
    @GetMapping("/minus/{id}")
    public String decrease(@PathVariable Integer id, HttpSession session) {
        Account account = getAccount(session);
        if (account == null) {
			return "redirect:/login";
		}

        CartDetail item = cartRepo.findById(id).orElse(null);
        if (item != null && item.getAccount().getId().equals(account.getId())) {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                cartRepo.save(item);
            } else {
                cartRepo.delete(item);
            }
        }
        return "redirect:/cart";
    }

    // ================= XOÁ SẢN PHẨM =================
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, HttpSession session) {
        Account account = getAccount(session);
        if (account == null) {
			return "redirect:/login";
		}

        CartDetail item = cartRepo.findById(id).orElse(null);
        if (item != null && item.getAccount().getId().equals(account.getId())) {
            cartRepo.delete(item);
        }
        return "redirect:/cart";
    }

    // ================= LẤY USER =================
    private Account getAccount(HttpSession session) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            account = (Account) session.getAttribute("user");
        }
        return account;
    }
}