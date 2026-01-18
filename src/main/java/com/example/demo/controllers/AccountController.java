package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate; // ✅ Import class này

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
import com.example.demo.service.MembershipService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AccountController {

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private HttpSession session;

    @Autowired
    private MembershipService membershipService;

    // --- TRANG TÀI KHOẢN ---
    @GetMapping("/account")
    public String accountPage(Model model) {
        Account acc = (Account) session.getAttribute("account");
        if (acc == null) {
            return "redirect:/login";
        }

        // 1. Lấy dữ liệu mới nhất từ DB
        acc = accountRepo.findById(acc.getId()).orElse(acc);

        // 2. Cập nhật hạng thành viên
        membershipService.updateMembershipLevel(acc);
        accountRepo.save(acc); 
        session.setAttribute("account", acc);

        // 3. Logic tính toán hiển thị Tiến độ & Số tiền
        long currentSpent = acc.getTotalSpending() == null ? 0 : acc.getTotalSpending();
        
        String nextLevelName = null;
        long nextLevelThreshold = 0;
        String currentBenefits = "Tích điểm đổi quà";

        if (currentSpent < 5000000) {
            nextLevelName = "Bạc";
            nextLevelThreshold = 5000000;
            currentBenefits = "Tích điểm đổi quà";
        } else if (currentSpent < 10000000) {
            nextLevelName = "Vàng";
            nextLevelThreshold = 10000000;
            currentBenefits = "Giảm 2% đơn hàng";
        } else if (currentSpent < 20000000) {
            nextLevelName = "Kim Cương";
            nextLevelThreshold = 20000000;
            currentBenefits = "Giảm 5% + Freeship";
        } else {
            currentBenefits = "Giảm 10% + Freeship + Quà sinh nhật";
        }

        if (nextLevelName != null) {
            long amountToNextLevel = nextLevelThreshold - currentSpent;
            int progressPercent = (int) ((currentSpent * 100) / nextLevelThreshold);

            model.addAttribute("nextLevelName", nextLevelName);
            model.addAttribute("amountToNextLevel", amountToNextLevel);
            model.addAttribute("progressPercent", progressPercent);
        }

        model.addAttribute("currentBenefits", currentBenefits);
        model.addAttribute("account", acc);

        return "client/account";
    }

    // --- CÁC HÀM UPDATE ---

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

    // ✅ ĐÃ SỬA LẠI HÀM NÀY ĐỂ DÙNG LocalDate
    @PostMapping("/account/update-birthday")
    public String updateBirthday(@RequestParam("birthday") String birthday, RedirectAttributes redirect) {
        Account acc = (Account) session.getAttribute("account");
        if (acc == null) return "redirect:/login";

        if (birthday == null || birthday.isEmpty()) {
            redirect.addFlashAttribute("error", "❌ Vui lòng chọn ngày sinh!");
            return "redirect:/account";
        }

        try {
            // SỬA LỖI Ở ĐÂY: Dùng LocalDate.parse thay vì java.sql.Date.valueOf
            acc.setBirthday(LocalDate.parse(birthday));
            
            accountRepo.save(acc);
            session.setAttribute("account", acc);
            redirect.addFlashAttribute("success", "✅ Cập nhật ngày sinh thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            redirect.addFlashAttribute("error", "⚠️ Định dạng ngày không hợp lệ!");
        }

        return "redirect:/account";
    }

    @PostMapping("/account/update-email")
    public String updateEmail(@RequestParam("email") String email, RedirectAttributes redirect) {
        Account acc = (Account) session.getAttribute("account");
        if (acc == null) return "redirect:/login";

        if (email == null || email.trim().isEmpty()) {
            redirect.addFlashAttribute("error", "❌ Email không được để trống!");
            return "redirect:/account";
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            redirect.addFlashAttribute("error", "⚠️ Email không hợp lệ!");
            return "redirect:/account";
        }

        acc.setEmail(email.trim());
        accountRepo.save(acc);
        session.setAttribute("account", acc);
        redirect.addFlashAttribute("success", "✅ Cập nhật email thành công!");
        return "redirect:/account";
    }

    @PostMapping("/account/update-phone")
    public String updatePhone(@RequestParam("phone") String phone, RedirectAttributes redirect) {
        Account acc = (Account) session.getAttribute("account");
        if (acc == null) return "redirect:/login";

        if (phone == null || phone.trim().isEmpty()) {
            redirect.addFlashAttribute("error", "❌ Số điện thoại không được để trống!");
            return "redirect:/account";
        } else if (!phone.matches("^0\\d{9}$")) {
            redirect.addFlashAttribute("error", "⚠️ Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0!");
            return "redirect:/account";
        }

        acc.setPhone(phone.trim());
        accountRepo.save(acc);
        session.setAttribute("account", acc);
        redirect.addFlashAttribute("success", "✅ Cập nhật số điện thoại thành công!");
        return "redirect:/account";
    }

    @PostMapping("/account/update-address")
    public String updateAddress(@RequestParam("address") String address, RedirectAttributes redirect) {
        Account acc = (Account) session.getAttribute("account");
        if (acc == null) return "redirect:/login";

        if (address == null || address.trim().isEmpty()) {
            redirect.addFlashAttribute("error", "❌ Địa chỉ không được để trống!");
            return "redirect:/account";
        }

        acc.setAddress(address.trim());
        accountRepo.save(acc);
        session.setAttribute("account", acc);
        redirect.addFlashAttribute("success", "✅ Cập nhật địa chỉ thành công!");
        return "redirect:/account";
    }

    @PostMapping("/account/update-password")
    public String updatePassword(@RequestParam("password") String password, RedirectAttributes redirect) {
        Account acc = (Account) session.getAttribute("account");
        if (acc == null) return "redirect:/login";

        if (password == null || password.trim().isEmpty()) {
            redirect.addFlashAttribute("error", "❌ Mật khẩu không được để trống!");
            return "redirect:/account";
        } else if (password.length() < 6) {
            redirect.addFlashAttribute("error", "⚠️ Mật khẩu phải có ít nhất 6 ký tự!");
            return "redirect:/account";
        }

        acc.setPassword(password);
        accountRepo.save(acc);
        session.setAttribute("account", acc);
        redirect.addFlashAttribute("success", "✅ Cập nhật mật khẩu thành công!");
        return "redirect:/account";
    }

    @PostMapping("/account/upload-avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile file, RedirectAttributes redirect) {
        Account acc = (Account) session.getAttribute("account");
        if (acc != null && !file.isEmpty()) {
            try {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path uploadDir = Paths.get("uploads/avatar/");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }
                Path filePath = uploadDir.resolve(fileName);
                Files.write(filePath, file.getBytes());
                acc.setPhoto("/images/avatar/" + fileName);
                accountRepo.save(acc);
                session.setAttribute("account", acc);
                redirect.addFlashAttribute("success", "✅ Ảnh đại diện đã được cập nhật!");
            } catch (IOException e) {
                e.printStackTrace();
                redirect.addFlashAttribute("error", "⚠️ Lỗi khi tải ảnh lên!");
            }
        } else {
            redirect.addFlashAttribute("error", "❌ Vui lòng chọn ảnh để tải lên!");
        }
        return "redirect:/account";
    }
}