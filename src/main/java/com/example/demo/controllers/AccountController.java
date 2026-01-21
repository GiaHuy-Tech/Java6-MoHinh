package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.OrdersRepository;
import com.example.demo.service.MembershipService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AccountController {

    @Autowired
    private AccountRepository accountRepo;
    
    @Autowired
    private OrdersRepository ordersRepo;

    @Autowired
    private HttpSession session;

    @Autowired
    private MembershipService membershipService;

    @GetMapping("/account")
    public String accountPage(Model model) {
        Account acc = (Account) session.getAttribute("account");
        if (acc == null) {
            return "redirect:/login";
        }

        // 1. Đồng bộ dữ liệu mới nhất
        acc = accountRepo.findById(acc.getId()).orElse(acc);

        // 2. Cập nhật hạng thành viên
        membershipService.updateMembershipLevel(acc);
        accountRepo.save(acc); 
        session.setAttribute("account", acc);

        // --- 🔥 LOGIC THỐNG KÊ (ĐÃ SỬA GỌN) 🔥 ---
        
        // Vì Repository giờ nhận Integer, ta truyền thẳng acc.getId() vào
        Long totalSpentDB = ordersRepo.sumTotalSpentByAccountId(acc.getId());
        Long totalOrdersDB = ordersRepo.countByAccountId(acc.getId());
        
        long totalSpent = (totalSpentDB != null) ? totalSpentDB : 0L;
        long orderCount = (totalOrdersDB != null) ? totalOrdersDB : 0L;
        long savedAmount = 0L;

        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("orderCount", orderCount);
        model.addAttribute("savedAmount", savedAmount);

        // --- TÍNH TIẾN ĐỘ LÊN HẠNG ---
        long currentSpentForLevel = totalSpent; 
        
        String nextLevelName = null;
        long nextLevelThreshold = 0;
        String currentBenefits = "Tích điểm đổi quà";

        if (currentSpentForLevel < 5000000) {
            nextLevelName = "Bạc";
            nextLevelThreshold = 5000000;
            currentBenefits = "Tích điểm đổi quà";
        } else if (currentSpentForLevel < 10000000) {
            nextLevelName = "Vàng";
            nextLevelThreshold = 10000000;
            currentBenefits = "Giảm 2% đơn hàng";
        } else if (currentSpentForLevel < 20000000) {
            nextLevelName = "Kim Cương";
            nextLevelThreshold = 20000000;
            currentBenefits = "Giảm 5% + Freeship";
        } else {
            currentBenefits = "Giảm 10% + Freeship + Quà sinh nhật";
        }

        if (nextLevelName != null) {
            long amountToNextLevel = nextLevelThreshold - currentSpentForLevel;
            int progressPercent = (nextLevelThreshold > 0) 
                                ? (int) ((currentSpentForLevel * 100) / nextLevelThreshold) 
                                : 100;

            model.addAttribute("nextLevelName", nextLevelName);
            model.addAttribute("amountToNextLevel", amountToNextLevel);
            model.addAttribute("progressPercent", progressPercent);
        }

        model.addAttribute("currentBenefits", currentBenefits);
        model.addAttribute("account", acc);

        return "client/account"; 
    }

    // ... (Giữ nguyên các hàm update bên dưới của bạn) ...
    // Copy lại các hàm @PostMapping update-fullname, password, avatar... y như cũ
    @PostMapping("/account/update-fullname")
    public String updateFullName(@RequestParam("fullName") String fullName, RedirectAttributes redirect) {
        Account acc = (Account) session.getAttribute("account");
        if (acc == null) return "redirect:/login";
        if (fullName == null || fullName.trim().isEmpty()) {
            redirect.addFlashAttribute("error", "❌ Họ tên không được để trống!");
            return "redirect:/account";
        }
        acc.setFullName(fullName.trim());
        accountRepo.save(acc);
        session.setAttribute("account", acc);
        redirect.addFlashAttribute("success", "✅ Cập nhật họ tên thành công!");
        return "redirect:/account";
    }
    // ... (Các hàm update khác giữ nguyên)
    @PostMapping("/account/upload-avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile file, RedirectAttributes redirect) {
        Account acc = (Account) session.getAttribute("account");
        if (acc != null && file != null && !file.isEmpty()) {
            try {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path uploadDir = Paths.get("uploads/avatar/");
                if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);
                Path filePath = uploadDir.resolve(fileName);
                Files.write(filePath, file.getBytes());
                acc.setPhoto("/images/avatar/" + fileName);
                accountRepo.save(acc);
                session.setAttribute("account", acc);
                redirect.addFlashAttribute("success", "✅ Ảnh đại diện đã được cập nhật!");
            } catch (IOException e) {
                e.printStackTrace();
                redirect.addFlashAttribute("error", "⚠️ Lỗi hệ thống khi lưu ảnh!");
            }
        } else {
            redirect.addFlashAttribute("error", "❌ Vui lòng chọn ảnh để tải lên!");
        }
        return "redirect:/account";
    }
}