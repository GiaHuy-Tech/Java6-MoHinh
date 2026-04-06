package com.example.demo.controllers;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.*;
import com.example.demo.repository.*;

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

    // ================= VIEW =================
    @GetMapping
    public String accountPage(Model model) {
        Account sessionAcc = getSessionAccount();
        if (sessionAcc == null) return "redirect:/login";

        Account account = accountRepo.findById(sessionAcc.getId()).orElse(null);
        if (account == null) return "redirect:/login";

        BigDecimal totalSpent = ordersRepo.sumTotalByAccountAndStatus(account.getId());
        if (totalSpent == null) totalSpent = BigDecimal.ZERO;

        account.setTotalSpending(totalSpent);

        String membershipName = "Đồng";
        if (totalSpent.compareTo(new BigDecimal("1000000000")) >= 0) membershipName = "Kim Cương";
        else if (totalSpent.compareTo(new BigDecimal("100000000")) >= 0) membershipName = "Bạch Kim";
        else if (totalSpent.compareTo(new BigDecimal("50000000")) >= 0) membershipName = "Vàng";
        else if (totalSpent.compareTo(new BigDecimal("10000000")) >= 0) membershipName = "Bạc";

        Membership membership = membershipRepo.findByName(membershipName).orElse(null);
        if (membership != null) {
            account.setMembership(membership);
        }
        accountRepo.save(account);

        List<Address> addresses = addressRepo.findByAccount_Id(account.getId());

        model.addAttribute("account", account);
        model.addAttribute("addresses", addresses);
        model.addAttribute("membershipName", membershipName);
        model.addAttribute("orderCount", ordersRepo.countByAccountId(account.getId()));
        model.addAttribute("totalSpent", totalSpent);

        return "client/account";
    }

    // ================= AVATAR =================
    @PostMapping("/upload-avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile file, RedirectAttributes redirect) {
        Account sessionAcc = getSessionAccount();
        if (sessionAcc == null) return "redirect:/login";

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

            Account dbAcc = accountRepo.findById(sessionAcc.getId()).get();
            dbAcc.setAvatar(fileName);
            accountRepo.save(dbAcc);

            session.setAttribute("account", dbAcc);
        } catch (IOException e) {
            redirect.addFlashAttribute("error", "Lỗi upload!");
        }
        return "redirect:/account";
    }

    // ================= ADD ADDRESS =================
    @PostMapping("/add-address")
    public String addAddress(
            @RequestParam String recipientName,
            @RequestParam String recipientPhone,
            @RequestParam String detail,
            @RequestParam String district,
            @RequestParam String province,
            @RequestParam(required = false) String ward) {

        Account acc = getSessionAccount();
        if (acc == null) return "redirect:/login";

        Address address = new Address();
        address.setAccount(accountRepo.findById(acc.getId()).get());
        address.setRecipientName(recipientName);
        address.setRecipientPhone(recipientPhone);
        address.setDetail(detail);
        address.setDistrict(district);
        address.setProvince(province);
        address.setWard(ward);
        address.setIsDefault(false);
        address.setIsActive(true);

        addressRepo.save(address);
        return "redirect:/account";
    }

    // ================= DELETE =================
    @PostMapping("/delete-address")
    public String deleteAddress(@RequestParam Long id) {
        Account acc = getSessionAccount();
        if (acc == null) return "redirect:/login";

        addressRepo.findByIdAndAccount_Id(id, acc.getId())
                .ifPresent(addressRepo::delete);

        return "redirect:/account";
    }

    // ================= SET DEFAULT =================
    @PostMapping("/set-default")
    public String setDefault(@RequestParam Long id) {
        Account acc = getSessionAccount();
        if (acc == null) return "redirect:/login";

        List<Address> list = addressRepo.findByAccount_Id(acc.getId());

        for (Address a : list) {
            a.setIsDefault(a.getId().equals(id));
        }

        addressRepo.saveAll(list);
        return "redirect:/account";
    }

    // ================= HELPER =================
    private Account getSessionAccount() {
        Account acc = (Account) session.getAttribute("account");
        return (acc != null) ? acc : (Account) session.getAttribute("user");
    }
}