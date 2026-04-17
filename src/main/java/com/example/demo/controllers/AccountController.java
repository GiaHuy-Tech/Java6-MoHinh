package com.example.demo.controllers;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

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

    // ================= VIEW =================
    @GetMapping
    public String accountPage(Model model) {
        Account sessionAcc = getSessionAccount();
        if (sessionAcc == null) {
			return "redirect:/login";
		}

        Account account = accountRepo.findById(sessionAcc.getId()).orElse(null);
        if (account == null) {
			return "redirect:/login";
		}

        // 1. Lấy tổng chi tiêu
        BigDecimal totalSpent = ordersRepo.sumTotalByAccountAndStatus(account.getId());
        if (totalSpent == null) {
			totalSpent = BigDecimal.ZERO;
		}

        account.setTotalSpending(totalSpent);

        // 2. QUY ĐỔI RA ĐIỂM (Tỷ lệ: 10.000đ = 1 Điểm)
        int currentPoints = totalSpent.divide(new BigDecimal("10000"), RoundingMode.DOWN).intValue();

        // 3. XẾP HẠNG THEO ĐIỂM (Chuẩn 100% theo Admin Dashboard)
        String membershipName = "Đồng";
        if (currentPoints >= 10000) {
            membershipName = "Kim Cương";
        } else if (currentPoints >= 5000) {
            membershipName = "Vàng";
        } else if (currentPoints >= 1000) {
            membershipName = "Bạc";
        }

        // 4. Lưu hạng vào Database
        Membership membership = membershipRepo.findByName(membershipName).orElse(null);
        if (membership != null) {
            account.setMembership(membership);
        }
        accountRepo.save(account);

        // LẤY DANH SÁCH ĐỊA CHỈ
        List<Address> addresses = addressRepo.findByAccount_Id(account.getId());

        model.addAttribute("account", account);
        model.addAttribute("addresses", addresses);
        model.addAttribute("membershipName", membershipName);
        model.addAttribute("orderCount", ordersRepo.countByAccountId(account.getId()));
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("currentPoints", currentPoints); // Gửi điểm ra giao diện

        return "client/account";
    }

    // ================= AVATAR =================
    @PostMapping("/upload-avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile file, RedirectAttributes redirect) {
        Account sessionAcc = getSessionAccount();
        if (sessionAcc == null) {
			return "redirect:/login";
		}

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
				Files.createDirectories(uploadPath);
			}

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

            Account dbAcc = accountRepo.findById(sessionAcc.getId()).get();
            dbAcc.setAvatar(fileName);
            accountRepo.save(dbAcc);

            session.setAttribute("account", dbAcc);
            redirect.addFlashAttribute("success", "Cập nhật ảnh đại diện thành công!");
        } catch (IOException e) {
            redirect.addFlashAttribute("error", "Lỗi upload!");
        }
        return "redirect:/account";
    }

    // ================= UPDATE INFO (MỚI BỔ SUNG) =================
    @PostMapping("/update-fullname")
    public String updateFullName(@RequestParam("fullName") String fullName, RedirectAttributes redirect) {
        Account sessionAcc = getSessionAccount();
        if (sessionAcc == null) {
			return "redirect:/login";
		}

        Account dbAcc = accountRepo.findById(sessionAcc.getId()).get();
        dbAcc.setFullName(fullName);
        accountRepo.save(dbAcc);

        session.setAttribute("account", dbAcc);
        redirect.addFlashAttribute("success", "Cập nhật họ tên thành công!");
        return "redirect:/account";
    }

    @PostMapping("/update-phone")
    public String updatePhone(@RequestParam("phone") String phone, RedirectAttributes redirect) {
        Account sessionAcc = getSessionAccount();
        if (sessionAcc == null) {
			return "redirect:/login";
		}

        Account dbAcc = accountRepo.findById(sessionAcc.getId()).get();
        dbAcc.setPhone(phone);
        accountRepo.save(dbAcc);

        session.setAttribute("account", dbAcc);
        redirect.addFlashAttribute("success", "Cập nhật số điện thoại thành công!");
        return "redirect:/account";
    }

    @PostMapping("/update-password")
    public String updatePassword(@RequestParam("password") String password, RedirectAttributes redirect) {
        Account sessionAcc = getSessionAccount();
        if (sessionAcc == null) {
			return "redirect:/login";
		}

        Account dbAcc = accountRepo.findById(sessionAcc.getId()).get();
        dbAcc.setPassword(password); // Lưu ý: Nếu có dùng Bcrypt thì nhớ hash password ở đây
        accountRepo.save(dbAcc);

        redirect.addFlashAttribute("success", "Đổi mật khẩu thành công!");
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
            @RequestParam(required = false) String ward,
            RedirectAttributes redirect) {

        Account acc = getSessionAccount();
        if (acc == null) {
			return "redirect:/login";
		}

        Address address = new Address();
        address.setAccount(accountRepo.findById(acc.getId()).get());
        address.setRecipientName(recipientName);
        address.setRecipientPhone(recipientPhone);
        address.setDetail(detail);
        address.setDistrict(district);
        address.setProvince(province);
        address.setWard(ward);

        // Nếu là địa chỉ đầu tiên thì set mặc định
        List<Address> addresses = addressRepo.findByAccount_Id(acc.getId());
        address.setIsDefault(addresses.isEmpty());
        address.setIsActive(true);

        addressRepo.save(address);
        redirect.addFlashAttribute("success", "Thêm địa chỉ mới thành công!");
        return "redirect:/account";
    }

    // ================= DELETE =================
    @PostMapping("/delete-address")
    public String deleteAddress(@RequestParam Long id, RedirectAttributes redirect) {
        Account acc = getSessionAccount();
        if (acc == null) {
			return "redirect:/login";
		}

        addressRepo.findByIdAndAccount_Id(id, acc.getId())
                .ifPresent(addressRepo::delete);

        redirect.addFlashAttribute("success", "Xóa địa chỉ thành công!");
        return "redirect:/account";
    }

    // ================= SET DEFAULT =================  ////
    @PostMapping("/set-default")
    public String setDefault(@RequestParam Long id, RedirectAttributes redirect) {
        Account acc = getSessionAccount();
        if (acc == null) {
			return "redirect:/login";
		}

        List<Address> list = addressRepo.findByAccount_Id(acc.getId());

        for (Address a : list) {
            a.setIsDefault(a.getId().equals(id));
        }

        addressRepo.saveAll(list);
        redirect.addFlashAttribute("success", "Đã đặt làm địa chỉ mặc định!");
        return "redirect:/account";
    }

    // ================= HELPER =================
    private Account getSessionAccount() {
        Account acc = (Account) session.getAttribute("account");
        return (acc != null) ? acc : (Account) session.getAttribute("user");
    }
}