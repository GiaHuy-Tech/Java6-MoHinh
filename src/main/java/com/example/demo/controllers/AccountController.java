package com.example.demo.controllers;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Account;
import com.example.demo.model.Address;
import com.example.demo.model.Membership;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.AddressRepository;
import com.example.demo.repository.MembershipRepository;
import com.example.demo.repository.OrdersRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/account")
public class AccountController {

    @Autowired private AccountRepository accountRepo;
    @Autowired private OrdersRepository ordersRepo;
    @Autowired private AddressRepository addressRepo;
    @Autowired private MembershipRepository membershipRepo;
    @Autowired private HttpSession session;

    private final String UPLOAD_DIR = "uploads/avatar/";

    @GetMapping
    public String accountPage(Model model) {
        Account sessionAcc = (Account) session.getAttribute("account");
        if (sessionAcc == null) sessionAcc = (Account) session.getAttribute("user");
        if (sessionAcc == null) return "redirect:/login";

        Account account = accountRepo.findById(sessionAcc.getId()).orElse(null);
        if (account == null) return "redirect:/login";

        // 1. TÍNH TỔNG CHI TIÊU THỰC TẾ (Chỉ tính đơn hàng HOÀN TẤT - Status 4)
        BigDecimal totalSpent = ordersRepo.sumTotalByAccountAndStatus(account.getId());
        if (totalSpent == null) totalSpent = BigDecimal.ZERO;

        // Cập nhật lại vào Database để đồng bộ dữ liệu
        account.setTotalSpending(totalSpent);

        // 2. XÁC ĐỊNH HẠNG DỰA TRÊN TỔNG CHI
        String membershipName = "Đồng";
        if (totalSpent.compareTo(new BigDecimal("1000000000")) >= 0) membershipName = "Kim Cương";
        else if (totalSpent.compareTo(new BigDecimal("100000000")) >= 0) membershipName = "Bạch Kim";
        else if (totalSpent.compareTo(new BigDecimal("50000000")) >= 0) membershipName = "Vàng";
        else if (totalSpent.compareTo(new BigDecimal("10000000")) >= 0) membershipName = "Bạc";

        // 3. CẬP NHẬT OBJECT MEMBERSHIP VÀO ACCOUNT
        Membership membership = membershipRepo.findByName(membershipName).orElse(null);
        if (membership != null) {
            account.setMembership(membership);
        }
        
        // Lưu lại thay đổi mới nhất vào DB
        accountRepo.save(account);

        // Đẩy dữ liệu ra View
        Address defaultAddress = addressRepo.findByAccount_IdAndIsDefaultTrue(account.getId()).orElse(null);
        model.addAttribute("account", account);
        model.addAttribute("defaultAddress", defaultAddress);
        model.addAttribute("membershipName", membershipName);
        model.addAttribute("orderCount", ordersRepo.countByAccountId(account.getId()));
        model.addAttribute("totalSpent", totalSpent);

        return "client/account";
    }

    @PostMapping("/upload-avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile file, RedirectAttributes redirect) {
        Account sessionAcc = getSessionAccount();
        if (sessionAcc == null) return "redirect:/login";

        if (file.isEmpty()) {
            redirect.addFlashAttribute("error", "Vui lòng chọn ảnh!");
            return "redirect:/account";
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            Account dbAcc = accountRepo.findById(sessionAcc.getId()).get();
            dbAcc.setAvatar(fileName); 
            accountRepo.save(dbAcc);

            session.setAttribute("account", dbAcc);
            redirect.addFlashAttribute("success", "Đổi ảnh đại diện thành công!");
        } catch (IOException e) {
            redirect.addFlashAttribute("error", "Lỗi lưu ảnh!");
        }
        return "redirect:/account";
    }

    @PostMapping("/update-fullname")
    public String updateFullName(@RequestParam("fullName") String fullName, RedirectAttributes redirect) {
        return updateAccountField(acc -> acc.setFullName(fullName.trim()), redirect, "Họ tên");
    }

    @PostMapping("/update-phone")
    public String updatePhone(@RequestParam("phone") String phone, RedirectAttributes redirect) {
        return updateAccountField(acc -> acc.setPhone(phone.trim()), redirect, "Số điện thoại");
    }

    @PostMapping("/update-address")
    public String updateAddress(@RequestParam("address") String addressDetail, RedirectAttributes redirect) {
        Account sessionAcc = getSessionAccount();
        if (sessionAcc == null) return "redirect:/login";

        Address address = addressRepo.findByAccount_IdAndIsDefaultTrue(sessionAcc.getId()).orElse(new Address());
        if (address.getId() == null) {
            Account dbAcc = accountRepo.findById(sessionAcc.getId()).get();
            address.setAccount(dbAcc);
            address.setIsDefault(true);
        }
        address.setDetail(addressDetail);
        addressRepo.save(address);

        redirect.addFlashAttribute("success", "Cập nhật địa chỉ thành công!");
        return "redirect:/account";
    }

    private String updateAccountField(java.util.function.Consumer<Account> updater, RedirectAttributes redirect, String fieldName) {
        Account sessionAcc = getSessionAccount();
        if (sessionAcc == null) return "redirect:/login";
        
        Account dbAcc = accountRepo.findById(sessionAcc.getId()).orElse(null);
        if (dbAcc != null) {
            updater.accept(dbAcc);
            accountRepo.save(dbAcc);
            session.setAttribute("account", dbAcc);
            redirect.addFlashAttribute("success", "Cập nhật " + fieldName + " thành công!");
        }
        return "redirect:/account";
    }

    private Account getSessionAccount() {
        Account acc = (Account) session.getAttribute("account");
        return (acc != null) ? acc : (Account) session.getAttribute("user");
    }
}