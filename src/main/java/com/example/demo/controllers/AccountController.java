package com.example.demo.controllers;

import java.io.IOException;
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
import com.example.demo.repository.OrdersRepository; // Đảm bảo bạn đã có repo này

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/account") // Prefix chung cho tất cả các đường dẫn
public class AccountController {

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private OrdersRepository ordersRepo;

    @Autowired
    private HttpSession session;

    // Đường dẫn lưu ảnh (Lưu vào thư mục static để hiển thị được ngay)
    private final String UPLOAD_DIR = "src/main/resources/static/images/avatar/";

    // ================== TRANG PROFILE ==================
    @GetMapping("")
    public String accountPage(Model model) {
        Account sessionAccount = (Account) session.getAttribute("account");
        if (sessionAccount == null) {
            return "redirect:/login";
        }

        // 1. Load lại Account từ DB để có dữ liệu mới nhất (tránh session bị cũ)
        Optional<Account> opt = accountRepo.findById(sessionAccount.getId());
        if (opt.isEmpty()) {
			return "redirect:/login";
		}
        Account acc = opt.get();

        // 2. Tính toán thống kê từ OrderRepository
        // Lưu ý: Account ID là Integer, Repository trả về Long hoặc null
        Long totalSpentRaw = ordersRepo.sumTotalSpentByAccountId(acc.getId());
        Long totalOrdersRaw = ordersRepo.countByAccountId(acc.getId());

        long totalSpent = (totalSpentRaw != null) ? totalSpentRaw : 0L;
        long orderCount = (totalOrdersRaw != null) ? totalOrdersRaw : 0L;
        // Giả sử logic tiết kiệm là 10% tổng chi tiêu (hoặc lấy từ DB nếu có)
        long savedAmount = totalSpent / 10;

        // 3. Cập nhật lại TotalSpending vào bảng Account nếu cần thiết
        if (!acc.getTotalSpending().equals(totalSpent)) {
            acc.setTotalSpending(totalSpent);
            accountRepo.save(acc);
        }

        // 4. Logic phân hạng thành viên & Quyền lợi
        String currentLevel = "Thành Viên Mới";
        String nextLevelName = null;
        long nextLevelThreshold = 0;
        String currentBenefits = "Ưu đãi thành viên mới";

        if (totalSpent < 5000000) {
            currentLevel = "Đồng";
            nextLevelName = "Bạc";
            nextLevelThreshold = 5000000;
            currentBenefits = "Tích điểm đổi quà";
        } else if (totalSpent < 10000000) {
            currentLevel = "Bạc";
            nextLevelName = "Vàng";
            nextLevelThreshold = 10000000;
            currentBenefits = "Giảm 2% mọi đơn hàng";
        } else if (totalSpent < 20000000) {
            currentLevel = "Vàng";
            nextLevelName = "Kim Cương";
            nextLevelThreshold = 20000000;
            currentBenefits = "Giảm 5% + Freeship";
        } else {
            currentLevel = "Kim Cương";
            currentBenefits = "Giảm 10% + Freeship + Quà sinh nhật";
            // Không còn cấp cao hơn
        }

        // Cập nhật hạng vào DB nếu khác
        if (!currentLevel.equals(acc.getMembershipLevel())) {
            acc.setMembershipLevel(currentLevel);
            accountRepo.save(acc);
        }

        // 5. Tính tiến độ
        if (nextLevelName != null) {
            long amountToNextLevel = nextLevelThreshold - totalSpent;
            // Tính phần trăm: (Tiêu hiện tại / Mốc kế tiếp) * 100
            int progressPercent = (int) ((totalSpent * 100) / nextLevelThreshold);

            model.addAttribute("nextLevelName", nextLevelName);
            model.addAttribute("amountToNextLevel", amountToNextLevel);
            model.addAttribute("progressPercent", progressPercent);
        }

        // 6. Đẩy dữ liệu ra View
        session.setAttribute("account", acc); // Cập nhật lại session
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("orderCount", orderCount);
        model.addAttribute("savedAmount", savedAmount);
        model.addAttribute("currentBenefits", currentBenefits);

        return "client/account"; // Trả về file HTML của bạn
    }

    // ================== CÁC CHỨC NĂNG CẬP NHẬT ==================

    @PostMapping("/update-fullname")
    public String updateFullName(@RequestParam("fullName") String fullName, RedirectAttributes redirect) {
        return updateAccountField(acc -> acc.setFullName(fullName.trim()), redirect, "Họ tên");
    }

    @PostMapping("/update-phone")
    public String updatePhone(@RequestParam("phone") String phone, RedirectAttributes redirect) {
        return updateAccountField(acc -> acc.setPhone(phone.trim()), redirect, "Số điện thoại");
    }

    @PostMapping("/update-email")
    public String updateEmail(@RequestParam("email") String email, RedirectAttributes redirect) {
        // Thực tế nên check email đã tồn tại hay chưa
        return updateAccountField(acc -> acc.setEmail(email.trim()), redirect, "Email");
    }

    @PostMapping("/update-address")
    public String updateAddress(@RequestParam("address") String address, RedirectAttributes redirect) {
        // Cập nhật địa chỉ mặc định trong bảng Account
        return updateAccountField(acc -> acc.setAddress(address.trim()), redirect, "Địa chỉ");
    }

    @PostMapping("/update-birthday")
    public String updateBirthday(@RequestParam("birthday") String birthdayStr, RedirectAttributes redirect) {
        // Model dùng LocalDate, View gửi String "yyyy-MM-dd"
        Account acc = getSessionAccount();
        if (acc == null) {
			return "redirect:/login";
		}

        try {
            LocalDate birthDay = LocalDate.parse(birthdayStr);
            acc.setBirthDay(birthDay);
            accountRepo.save(acc);
            session.setAttribute("account", acc);
            redirect.addFlashAttribute("success", "Cập nhật ngày sinh thành công!");
        } catch (DateTimeParseException e) {
            redirect.addFlashAttribute("error", "Định dạng ngày không hợp lệ!");
        }
        return "redirect:/account";
    }

    @PostMapping("/update-password")
    public String updatePassword(@RequestParam("password") String password, RedirectAttributes redirect) {
        // Ở đây nên mã hóa password trước khi lưu (BCrypt)
        return updateAccountField(acc -> acc.setPassword(password), redirect, "Mật khẩu");
    }

    @PostMapping("/upload-avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile file, RedirectAttributes redirect) {
        Account acc = getSessionAccount();
        if (acc == null) {
			return "redirect:/login";
		}

        if (file.isEmpty()) {
            redirect.addFlashAttribute("error", "Vui lòng chọn ảnh!");
            return "redirect:/account";
        }

        try {
            // Tạo thư mục nếu chưa có
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Tạo tên file unique
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            // Lưu file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Lưu đường dẫn vào DB (dạng web path)
            acc.setPhoto("/images/avatar/" + fileName);
            accountRepo.save(acc);
            session.setAttribute("account", acc);

            redirect.addFlashAttribute("success", "Đổi ảnh đại diện thành công!");

        } catch (IOException e) {
            e.printStackTrace();
            redirect.addFlashAttribute("error", "Lỗi khi lưu ảnh: " + e.getMessage());
        }

        return "redirect:/account";
    }

    // ================== HELPER METHODS ==================

    // Hàm lấy account từ session
    private Account getSessionAccount() {
        return (Account) session.getAttribute("account");
    }

    // Interface functional để hỗ trợ hàm update chung
    private interface AccountUpdater {
        void update(Account acc);
    }

    // Hàm update chung để tránh lặp code
    private String updateAccountField(AccountUpdater updater, RedirectAttributes redirect, String fieldName) {
        Account sessionAcc = getSessionAccount();
        if (sessionAcc == null) {
			return "redirect:/login";
		}

        // Lấy object từ DB để đảm bảo session không đè dữ liệu cũ
        Optional<Account> opt = accountRepo.findById(sessionAcc.getId());
        if (opt.isPresent()) {
            Account dbAcc = opt.get();
            updater.update(dbAcc); // Chạy logic setter
            accountRepo.save(dbAcc); // Lưu DB
            session.setAttribute("account", dbAcc); // Cập nhật session
            redirect.addFlashAttribute("success", "Cập nhật " + fieldName + " thành công!");
        } else {
            redirect.addFlashAttribute("error", "Tài khoản không tồn tại!");
        }
        return "redirect:/account";
    }
}