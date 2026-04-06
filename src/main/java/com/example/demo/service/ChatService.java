package com.example.demo.service;

import com.example.demo.model.Products;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ChatService {

    @Autowired
    private ProductRepository productRepo;

    public String chat(String msg) {

        if (msg == null || msg.trim().isEmpty()) {
            return "Bạn nhập gì đó đi 😅";
        }

        msg = msg.toLowerCase();

        // ===== LOAD DATA 1 LẦN =====
        List<Products> products = productRepo.findAll();

        // ===== CHÀO =====
        if (msg.contains("hi") || msg.contains("hello") || msg.contains("chào")) {
            return "Chào bạn 👋 mình có thể giúp gì? (bạn hỏi về sản phẩm, giá, bán chạy...)";
        }

        // ===== CÒN HÀNG =====
        if (msg.contains("còn hàng")) {
            StringBuilder res = new StringBuilder("📦 Sản phẩm còn hàng:\n");
            int count = 0;

            for (Products p : products) {
                if (p.getQuantity() > 0 && count < 5) {
                    res.append("- ").append(p.getName()).append("\n");
                    count++;
                }
            }

            return count == 0 ? "Hiện không có sản phẩm nào còn hàng 😢" : res.toString();
        }

        // ===== HẾT HÀNG =====
        if (msg.contains("hết hàng")) {
            StringBuilder res = new StringBuilder("❌ Sản phẩm hết hàng:\n");
            int count = 0;

            for (Products p : products) {
                if (p.getQuantity() == 0 && count < 5) {
                    res.append("- ").append(p.getName()).append("\n");
                    count++;
                }
            }

            return count == 0 ? "Hiện chưa có sản phẩm nào hết hàng 👍" : res.toString();
        }

        // ===== BÁN CHẠY NHẤT =====
        if (msg.contains("bán chạy nhất")) {

            Products top = products.stream()
                    .max(Comparator.comparingInt(p -> p.getSold() == null ? 0 : p.getSold()))
                    .orElse(null);

            if (top == null) return "Chưa có dữ liệu 😅";

            return "🔥 Sản phẩm bán chạy nhất:\n"
                    + top.getName()
                    + "\n💰 Giá: " + top.getPrice() + "₫"
                    + "\n📈 Đã bán: " + top.getSold();
        }

        // ===== TOP BÁN CHẠY =====
        if (msg.contains("bán chạy") || msg.contains("hot")) {

            products.sort((a, b) -> {
                int soldA = a.getSold() == null ? 0 : a.getSold();
                int soldB = b.getSold() == null ? 0 : b.getSold();
                return soldB - soldA;
            });

            StringBuilder res = new StringBuilder("🔥 Top sản phẩm bán chạy:\n");

            for (int i = 0; i < Math.min(5, products.size()); i++) {
                Products p = products.get(i);
                res.append("- ")
                   .append(p.getName())
                   .append(" (")
                   .append(p.getSold())
                   .append(" đã bán)\n");
            }

            return res.toString();
        }

        // ===== YÊU THÍCH NHẤT =====
        if (msg.contains("yêu thích")) {

            Products fav = products.stream()
                    .max(Comparator.comparingInt(p -> p.getSold() == null ? 0 : p.getSold()))
                    .orElse(null);

            if (fav == null) return "Chưa có dữ liệu 😅";

            return "❤️ Sản phẩm được yêu thích nhất:\n"
                    + fav.getName()
                    + "\n💰 Giá: " + fav.getPrice() + "₫"
                    + "\n📈 Đã bán: " + fav.getSold();
        }

        // ===== GIÁ =====
        if (msg.contains("giá") || msg.contains("bao nhiêu")) {
            return "Bạn có thể xem giá trực tiếp trong từng sản phẩm nhé 😎";
        }

        // ===== ĐĂNG KÝ =====
        if (msg.contains("đăng ký")) {
            return "👉 Vào trang Đăng ký → nhập email + mật khẩu → bấm đăng ký 👍";
        }

        // ===== ĐỔI MẬT KHẨU =====
        if (msg.contains("đổi mật khẩu")) {
            return "👉 Vào tài khoản → chọn đổi mật khẩu → nhập mật khẩu mới 👍";
        }

        // ===== TÌM THEO TÊN =====
        for (Products p : products) {
            if (msg.contains(p.getName().toLowerCase())) {
                return "🔎 " + p.getName()
                        + "\n💰 Giá: " + p.getPrice() + "₫"
                        + "\n📦 Tình trạng: " + (p.getQuantity() > 0 ? "Còn hàng" : "Hết hàng")
                        + "\n📈 Đã bán: " + p.getSold();
            }
        }

        // ===== DEFAULT =====
        return "🤖 Bạn có thể hỏi:\n"
                + "- sản phẩm còn hàng\n"
                + "- sản phẩm hết hàng\n"
                + "- sản phẩm bán chạy\n"
                + "- sản phẩm bán chạy nhất\n"
                + "- sản phẩm yêu thích nhất\n"
                + "- giá sản phẩm\n"
                + "- đăng ký / đổi mật khẩu 😄";
    }
}