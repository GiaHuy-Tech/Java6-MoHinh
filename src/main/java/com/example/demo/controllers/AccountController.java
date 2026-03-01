package com.example.demo.controllers;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.OrdersRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private OrdersRepository ordersRepo;

    @Autowired
    private HttpSession session;

    private final String UPLOAD_DIR = "src/main/resources/static/images/avatar/";

    // ================== PROFILE PAGE ==================
    @GetMapping("")
    public String accountPage(Model model) {

        Account sessionAccount = (Account) session.getAttribute("account");
        if (sessionAccount == null) {
            return "redirect:/login";
        }

        Optional<Account> opt = accountRepo.findById(sessionAccount.getId());
        if (opt.isEmpty()) {
            return "redirect:/login";
        }

        Account acc = opt.get();

        // ====== TÍNH THỐNG KÊ ======
        Long totalSpentRaw = ordersRepo.sumTotalSpentByAccountId(acc.getId());
        BigDecimal totalSpent = (totalSpentRaw != null)
                ? BigDecimal.valueOf(totalSpentRaw)
                : BigDecimal.ZERO;

        Long totalOrdersRaw = ordersRepo.countByAccountId(acc.getId());
        long orderCount = (totalOrdersRaw != null) ? totalOrdersRaw : 0L;

        BigDecimal savedAmount = totalSpent.divide(BigDecimal.TEN);

        // ====== UPDATE totalSpending nếu thay đổi ======
        if (acc.getTotalSpending() == null ||
            acc.getTotalSpending().compareTo(totalSpent) != 0) {

            acc.setTotalSpending(totalSpent);
            accountRepo.save(acc);
        }

        // ====== PHÂN HẠNG (CHỈ HIỂN THỊ, KHÔNG LƯU DB) ======
        String currentLevel;
        String currentBenefits;

        if (totalSpent.compareTo(BigDecimal.valueOf(5_000_000)) < 0) {
            currentLevel = "Đồng";
            currentBenefits = "Tích điểm đổi quà";
        } else if (totalSpent.compareTo(BigDecimal.valueOf(10_000_000)) < 0) {
            currentLevel = "Bạc";
            currentBenefits = "Giảm 2% mọi đơn hàng";
        } else if (totalSpent.compareTo(BigDecimal.valueOf(20_000_000)) < 0) {
            currentLevel = "Vàng";
            currentBenefits = "Giảm 5% + Freeship";
        } else {
            currentLevel = "Kim Cương";
            currentBenefits = "Giảm 10% + Freeship + Quà sinh nhật";
        }

        // ====== ĐẨY DATA RA VIEW ======
        session.setAttribute("account", acc);

        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("orderCount", orderCount);
        model.addAttribute("savedAmount", savedAmount);
        model.addAttribute("currentLevel", currentLevel);
        model.addAttribute("currentBenefits", currentBenefits);

        return "client/account";
    }

    // ================== UPDATE FULLNAME ==================
    @PostMapping("/update-fullname")
    public String updateFullName(@RequestParam("fullName") String fullName,
                                 RedirectAttributes redirect) {
        return updateAccountField(acc -> acc.setFullName(fullName.trim()),
                redirect, "Họ tên");
    }

    // ================== UPDATE PHONE ==================
    @PostMapping("/update-phone")
    public String updatePhone(@RequestParam("phone") String phone,
                              RedirectAttributes redirect) {
        return updateAccountField(acc -> acc.setPhone(phone.trim()),
                redirect, "Số điện thoại");
    }

    // ================== UPDATE EMAIL ==================
    @PostMapping("/update-email")
    public String updateEmail(@RequestParam("email") String email,
                              RedirectAttributes redirect) {
        return updateAccountField(acc -> acc.setEmail(email.trim()),
                redirect, "Email");
    }

    // ================== UPDATE BIRTHDAY ==================
    @PostMapping("/update-birthday")
    public String updateBirthday(@RequestParam("birthday") String birthdayStr,
                                 RedirectAttributes redirect) {

        Account acc = getSessionAccount();
        if (acc == null) {
            return "redirect:/login";
        }

        try {
            LocalDate birthDay = LocalDate.parse(birthdayStr);
            acc.setBirthDay(birthDay);
            accountRepo.save(acc);
            session.setAttribute("account", acc);
            redirect.addFlashAttribute("success",
                    "Cập nhật ngày sinh thành công!");
        } catch (DateTimeParseException e) {
            redirect.addFlashAttribute("error",
                    "Định dạng ngày không hợp lệ!");
        }

        return "redirect:/account";
    }

    // ================== UPDATE PASSWORD ==================
    @PostMapping("/update-password")
    public String updatePassword(@RequestParam("password") String password,
                                 RedirectAttributes redirect) {

        return updateAccountField(acc -> acc.setPassword(password),
                redirect, "Mật khẩu");
    }

    // ================== UPLOAD AVATAR ==================
    @PostMapping("/upload-avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile file,
                               RedirectAttributes redirect) {

        Account acc = getSessionAccount();
        if (acc == null) {
            return "redirect:/login";
        }

        if (file.isEmpty()) {
            redirect.addFlashAttribute("error", "Vui lòng chọn ảnh!");
            return "redirect:/account";
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = System.currentTimeMillis()
                    + "_" + file.getOriginalFilename();

            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(),
                    filePath,
                    StandardCopyOption.REPLACE_EXISTING);

            // LƯU AVATAR (KHÔNG PHẢI PHOTO)
            acc.setAvatar("/images/avatar/" + fileName);
            accountRepo.save(acc);
            session.setAttribute("account", acc);

            redirect.addFlashAttribute("success",
                    "Đổi ảnh đại diện thành công!");

        } catch (IOException e) {
            e.printStackTrace();
            redirect.addFlashAttribute("error",
                    "Lỗi khi lưu ảnh!");
        }

        return "redirect:/account";
    }

    // ================== HELPER ==================
    private Account getSessionAccount() {
        return (Account) session.getAttribute("account");
    }

    private interface AccountUpdater {
        void update(Account acc);
    }

    private String updateAccountField(AccountUpdater updater,
                                      RedirectAttributes redirect,
                                      String fieldName) {

        Account sessionAcc = getSessionAccount();
        if (sessionAcc == null) {
            return "redirect:/login";
        }

        Optional<Account> opt =
                accountRepo.findById(sessionAcc.getId());

        if (opt.isPresent()) {

            Account dbAcc = opt.get();
            updater.update(dbAcc);
            accountRepo.save(dbAcc);
            session.setAttribute("account", dbAcc);

            redirect.addFlashAttribute("success",
                    "Cập nhật " + fieldName + " thành công!");

        } else {
            redirect.addFlashAttribute("error",
                    "Tài khoản không tồn tại!");
        }

        return "redirect:/account";
    }
}