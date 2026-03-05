package com.example.demo.controllers;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Account;
import com.example.demo.model.Address;
import com.example.demo.model.Orders;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.AddressRepository;
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
    private AddressRepository addressRepo;

    @Autowired
    private HttpSession session;

    private final String UPLOAD_DIR =
            "src/main/resources/static/images/avatar/";

    // =====================================================
    // ================= PROFILE PAGE ======================
    // =====================================================

    @GetMapping("")
    public String accountPage(Model model) {

        Account sessionAcc =
                (Account) session.getAttribute("account");

        if (sessionAcc == null) {
            return "redirect:/login";
        }

        Account acc =
                accountRepo.findById(sessionAcc.getId()).orElse(null);

        if (acc == null) {
            return "redirect:/login";
        }

        // ============================
        // LẤY DANH SÁCH ĐƠN HÀNG USER
        // ============================
        List<Orders> orders =
                ordersRepo.findByAccount_IdOrderByCreatedDateDesc(acc.getId());

        long orderCount = orders.size();

        // ============================
        // TÍNH TỔNG CHI TIÊU
        // ============================
        BigDecimal totalSpent = BigDecimal.ZERO;

        for (Orders o : orders) {
            if (o.getStatus() == 3 && o.getTotal() != null) {
                totalSpent = totalSpent.add(o.getTotal());
            }
        }

        acc.setTotalSpending(totalSpent);
        accountRepo.save(acc);

        // ============================
        // PHÂN HẠNG
        // ============================
        String currentLevel;

        if (totalSpent.compareTo(BigDecimal.valueOf(5_000_000)) < 0) {
            currentLevel = "Đồng";
        } else if (totalSpent.compareTo(BigDecimal.valueOf(10_000_000)) < 0) {
            currentLevel = "Bạc";
        } else if (totalSpent.compareTo(BigDecimal.valueOf(20_000_000)) < 0) {
            currentLevel = "Vàng";
        } else {
            currentLevel = "Kim Cương";
        }

        session.setAttribute("account", acc);

        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("orderCount", orderCount);
        model.addAttribute("currentLevel", currentLevel);

        return "client/account";
    }

    // =====================================================
    // ================= UPDATE INFO =======================
    // =====================================================

    @PostMapping("/update-fullname")
    public String updateFullName(
            @RequestParam("fullName") String fullName,
            RedirectAttributes redirect) {

        return updateAccountField(
                acc -> acc.setFullName(fullName.trim()),
                redirect,
                "Họ tên");
    }

    @PostMapping("/update-phone")
    public String updatePhone(
            @RequestParam("phone") String phone,
            RedirectAttributes redirect) {

        return updateAccountField(
                acc -> acc.setPhone(phone.trim()),
                redirect,
                "Số điện thoại");
    }

    @PostMapping("/update-birthday")
    public String updateBirthday(
            @RequestParam("birthday") String birthdayStr,
            RedirectAttributes redirect) {

        Account acc = getSessionAccount();

        if (acc == null)
            return "redirect:/login";

        try {
            acc.setBirthDay(LocalDate.parse(birthdayStr));
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

    @PostMapping("/update-password")
    public String updatePassword(
            @RequestParam("password") String password,
            RedirectAttributes redirect) {

        return updateAccountField(
                acc -> acc.setPassword(password),
                redirect,
                "Mật khẩu");
    }

    // =====================================================
    // ================= UPDATE ADDRESS ====================
    // =====================================================

    @PostMapping("/update-address")
    public String updateAddress(
            @RequestParam("address") String addressDetail,
            RedirectAttributes redirect) {

        Account acc = getSessionAccount();

        if (acc == null)
            return "redirect:/login";

        Optional<Address> optionalAddress =
                addressRepo.findByAccount_IdAndIsDefaultTrue(acc.getId());

        Address address;

        if (optionalAddress.isPresent()) {
            address = optionalAddress.get();
            address.setDetail(addressDetail);
        } else {
            address = new Address();
            address.setAccount(acc);
            address.setDetail(addressDetail);
            address.setIsDefault(true);
        }

        addressRepo.save(address);

        redirect.addFlashAttribute("success",
                "Cập nhật địa chỉ thành công!");

        return "redirect:/account";
    }

    // =====================================================
    // ================= UPLOAD AVATAR =====================
    // =====================================================

    @PostMapping("/upload-avatar")
    public String uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            RedirectAttributes redirect) {

        Account acc = getSessionAccount();

        if (acc == null)
            return "redirect:/login";

        if (file.isEmpty()) {
            redirect.addFlashAttribute("error",
                    "Vui lòng chọn ảnh!");
            return "redirect:/account";
        }

        try {

            Path uploadPath = Paths.get(UPLOAD_DIR);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName =
                    System.currentTimeMillis()
                            + "_" + file.getOriginalFilename();

            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(),
                    filePath,
                    StandardCopyOption.REPLACE_EXISTING);

            acc.setAvatar("/images/avatar/" + fileName);

            accountRepo.save(acc);
            session.setAttribute("account", acc);

            redirect.addFlashAttribute("success",
                    "Đổi ảnh đại diện thành công!");

        } catch (IOException e) {
            redirect.addFlashAttribute("error",
                    "Lỗi khi lưu ảnh!");
        }

        return "redirect:/account";
    }

    // =====================================================
    // ================= HELPER ============================
    // =====================================================

    private Account getSessionAccount() {
        return (Account) session.getAttribute("account");
    }

    private interface AccountUpdater {
        void update(Account acc);
    }

    private String updateAccountField(
            AccountUpdater updater,
            RedirectAttributes redirect,
            String fieldName) {

        Account sessionAcc = getSessionAccount();

        if (sessionAcc == null)
            return "redirect:/login";

        Account dbAcc =
                accountRepo.findById(sessionAcc.getId()).orElse(null);

        if (dbAcc != null) {

            updater.update(dbAcc);
            accountRepo.save(dbAcc);
            session.setAttribute("account", dbAcc);

            redirect.addFlashAttribute("success",
                    "Cập nhật " + fieldName + " thành công!");
        }

        return "redirect:/account";
    }
}