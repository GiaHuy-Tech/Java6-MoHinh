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
import org.springframework.data.domain.Sort;
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

    // ================= VIEW GIAO DIỆN HỒ SƠ =================
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

        // 1. Lấy tổng chi tiêu của tài khoản (Các đơn hàng đã hoàn thành)
        BigDecimal totalSpent = ordersRepo.sumTotalByAccountAndStatus(account.getId());
        if (totalSpent == null) {
			totalSpent = BigDecimal.ZERO;
		}
        account.setTotalSpending(totalSpent);

        // 2. QUY ĐỔI RA ĐIỂM (Tỷ lệ: 10.000đ = 1 Điểm)
        int currentPoints = totalSpent.divide(new BigDecimal("10000"), RoundingMode.DOWN).intValue();

        // 3. XẾP HẠNG ĐỘNG DỰA VÀO DATABASE CỦA ADMIN
        // Kéo toàn bộ danh sách Hạng từ DB về, sắp xếp từ Điểm Cao Nhất -> Thấp Nhất
        List<Membership> memberships = membershipRepo.findAll(Sort.by(Sort.Direction.DESC, "pointRequired"));
        
        Membership assignedMembership = null;
        for (Membership m : memberships) {
            // So sánh điểm hiện tại của User với mốc điểm Admin cài đặt
            if (currentPoints >= m.getPointRequired()) {
                assignedMembership = m;
                break; // Đạt hạng cao nhất có thể thì dừng vòng lặp
            }
        }

        // Nếu điểm user thấp hơn tất cả các mốc (Fallback) -> Gán cho hạng thấp nhất ở cuối mảng
        if (assignedMembership == null && !memberships.isEmpty()) {
            assignedMembership = memberships.get(memberships.size() - 1);
        }

        // Lấy tên hạng để gửi ra file HTML hiển thị màu sắc CSS
        String membershipName = assignedMembership != null ? assignedMembership.getName() : "Thành viên";

        // 4. Lưu Hạng mới vào bảng Account trong Database
        if (assignedMembership != null) {
            account.setMembership(assignedMembership);
        }
        accountRepo.save(account);

        // 5. LẤY DANH SÁCH SỔ ĐỊA CHỈ
        List<Address> addresses = addressRepo.findByAccount_Id(account.getId());

        // GỬI DATA RA HTML (Đồng bộ 100% với các biến bạn đã viết trên file HTML)
        model.addAttribute("account", account);
        model.addAttribute("addresses", addresses);
        model.addAttribute("membershipName", membershipName);
        model.addAttribute("orderCount", ordersRepo.countByAccountId(account.getId()));
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("currentPoints", currentPoints);

        return "client/account";
    }

    // ================= CẬP NHẬT ẢNH ĐẠI DIỆN =================
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

    // ================= CẬP NHẬT THÔNG TIN CÁ NHÂN =================
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

    // ================= BẢO MẬT: ĐỔI MẬT KHẨU =================
    @PostMapping("/update-password")
    public String updatePassword(@RequestParam("password") String password, RedirectAttributes redirect) {
        Account sessionAcc = getSessionAccount();
        if (sessionAcc == null) {
			return "redirect:/login";
		}

        Account dbAcc = accountRepo.findById(sessionAcc.getId()).get();
        dbAcc.setPassword(password); // Lưu ý: Nếu hệ thống bạn dùng Bcrypt thì nhớ hash password ở đây
        accountRepo.save(dbAcc);

        redirect.addFlashAttribute("success", "Đổi mật khẩu thành công!");
        return "redirect:/account";
    }

    // ================= SỔ ĐỊA CHỈ =================
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

        // Nếu đây là địa chỉ đầu tiên được thêm -> Đặt làm mặc định
        List<Address> addresses = addressRepo.findByAccount_Id(acc.getId());
        address.setIsDefault(addresses.isEmpty());
        address.setIsActive(true);

        addressRepo.save(address);
        redirect.addFlashAttribute("success", "Thêm địa chỉ mới thành công!");
        return "redirect:/account";
    }

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

    @PostMapping("/set-default")
    public String setDefault(@RequestParam Long id, RedirectAttributes redirect) {
        Account acc = getSessionAccount();
        if (acc == null) {
			return "redirect:/login";
		}

        List<Address> list = addressRepo.findByAccount_Id(acc.getId());

        // Đặt false cho tất cả, chỉ đặt true cho ID được chọn
        for (Address a : list) {
            a.setIsDefault(a.getId().equals(id));
        }

        addressRepo.saveAll(list);
        redirect.addFlashAttribute("success", "Đã đặt làm địa chỉ mặc định!");
        return "redirect:/account";
    }

    // ================= HELPER CHUNG =================
    private Account getSessionAccount() {
        Account acc = (Account) session.getAttribute("account");
        return (acc != null) ? acc : (Account) session.getAttribute("user");
    }
}