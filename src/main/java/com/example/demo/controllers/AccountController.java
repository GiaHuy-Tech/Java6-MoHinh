package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Account;
import com.example.demo.model.Address;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.AddressRepository;
import com.example.demo.repository.OrdersRepository;
import com.example.demo.service.MembershipService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AccountController {

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private AddressRepository addressRepo;

    @Autowired
    private OrdersRepository ordersRepo;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private HttpSession session;

    /* ================== ACCOUNT PAGE ================== */

    @GetMapping("/account")
    public String accountPage(Model model) {

        Account acc = (Account) session.getAttribute("account");
        if (acc == null) return "redirect:/login";

        acc = accountRepo.findById(acc.getId()).orElse(acc);

        membershipService.updateMembershipLevel(acc);
        accountRepo.save(acc);
        session.setAttribute("account", acc);

        // ==== Thống kê ====
        Long totalSpentDB = ordersRepo.sumTotalSpentByAccountId(acc.getId());
        Long totalOrdersDB = ordersRepo.countByAccountId(acc.getId());

        long totalSpent = (totalSpentDB != null) ? totalSpentDB : 0L;
        long orderCount = (totalOrdersDB != null) ? totalOrdersDB : 0L;

        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("orderCount", orderCount);

        // ==== Load Address ====
        List<Address> addresses = addressRepo.findByAccountId(acc.getId());
        model.addAttribute("addresses", addresses);

        model.addAttribute("account", acc);

        return "client/account";
    }

    /* ================== UPDATE FULLNAME ================== */

    @PostMapping("/account/update-fullname")
    public String updateFullName(@RequestParam("fullName") String fullName,
                                 RedirectAttributes redirect) {

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

    /* ================== UPLOAD AVATAR ================== */

    @PostMapping("/account/upload-avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile file,
                               RedirectAttributes redirect) {

        Account acc = (Account) session.getAttribute("account");

        if (acc != null && file != null && !file.isEmpty()) {
            try {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

                Path uploadDir = Paths.get("uploads/avatar/");
                if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);

                Path filePath = uploadDir.resolve(fileName);
                Files.write(filePath, file.getBytes());

                acc.setPhoto("/uploads/avatar/" + fileName);
                accountRepo.save(acc);
                session.setAttribute("account", acc);

                redirect.addFlashAttribute("success", "✅ Ảnh đại diện đã được cập nhật!");

            } catch (IOException e) {
                e.printStackTrace();
                redirect.addFlashAttribute("error", "⚠️ Lỗi hệ thống khi lưu ảnh!");
            }
        } else {
            redirect.addFlashAttribute("error", "❌ Vui lòng chọn ảnh!");
        }

        return "redirect:/account";
    }

    /* ================== ADD ADDRESS ================== */

    @GetMapping("/account/address/add-form")
    public String showAddAddressForm(Model model) {

        Account acc = (Account) session.getAttribute("account");
        if (acc == null) return "redirect:/login";

        model.addAttribute("address", new Address());

        return "client/address";   // file: templates/client/address.html
    }
    
    @PostMapping("/account/address/add")
    public String saveAddress(Address address,
                              RedirectAttributes redirect) {

        Account acc = (Account) session.getAttribute("account");
        if (acc == null) return "redirect:/login";

        List<Address> list = addressRepo.findByAccountId(acc.getId());

        if (list.size() >= 4) {
            redirect.addFlashAttribute("error", "⚠️ Chỉ tối đa 4 địa chỉ!");
            return "redirect:/account";
        }

        address.setAccount(acc);

        if (list.isEmpty()) {
            address.setIsDefault(true);
        }

        addressRepo.save(address);

        redirect.addFlashAttribute("success", "✅ Thêm địa chỉ thành công!");
        return "redirect:/account";
    }

    /* ================== DELETE ADDRESS ================== */

    @GetMapping("/account/address/delete/{id}")
    public String deleteAddress(@PathVariable("id") Integer id,
                                RedirectAttributes redirect) {

        Account acc = (Account) session.getAttribute("account");
        if (acc == null) return "redirect:/login";

        Address address = addressRepo.findById(id).orElse(null);
        if (address != null && address.getAccount().getId().equals(acc.getId())) {

        	boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
            addressRepo.delete(address);

            // Nếu xóa default thì set cái khác làm default
            if (wasDefault) {
                List<Address> list = addressRepo.findByAccountId(acc.getId());
                if (!list.isEmpty()) {
                    list.get(0).setIsDefault(true);
                    addressRepo.save(list.get(0));
                }
            }

            redirect.addFlashAttribute("success", "🗑️ Đã xóa địa chỉ!");
        }

        return "redirect:/account";
    }

    /* ================== SET DEFAULT ADDRESS ================== */

    @GetMapping("/account/address/default/{id}")
    public String setDefaultAddress(@PathVariable("id") Long id) {

        Account acc = (Account) session.getAttribute("account");
        if (acc == null) return "redirect:/login";

        List<Address> list = addressRepo.findByAccountId(acc.getId());

        for (Address a : list) {
            a.setIsDefault(a.getId().equals(id));
            addressRepo.save(a);
        }

        return "redirect:/account";
    }
}