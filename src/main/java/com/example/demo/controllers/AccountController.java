package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // ✅ thêm import này

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class AccountController {

	@Autowired
	private AccountRepository accountRepo;

	@Autowired
	private HttpSession session;

	// Trang thông tin tài khoản
	@GetMapping("/account")
	public String accountPage(Model model) {
		Account acc = (Account) session.getAttribute("account");
		if (acc == null) {
			return "redirect:/login";
		}
		model.addAttribute("account", acc);
		return "client/account";
	}

	@PostMapping("/account/update-fullname")
	public String updateFullName(@RequestParam("fullName") String fullName, Model model, RedirectAttributes redirect) {
		Account acc = (Account) session.getAttribute("account");
		if (acc == null)
			return "redirect:/login";

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

	@PostMapping("/account/update-email")
	public String updateEmail(@RequestParam("email") String email, Model model, RedirectAttributes redirect) {
		Account acc = (Account) session.getAttribute("account");
		if (acc == null)
			return "redirect:/login";

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
	public String updatePhone(@RequestParam("phone") String phone, Model model, RedirectAttributes redirect) {
		Account acc = (Account) session.getAttribute("account");
		if (acc == null)
			return "redirect:/login";

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
	public String updateAddress(@RequestParam("address") String address, Model model, RedirectAttributes redirect) {
		Account acc = (Account) session.getAttribute("account");
		if (acc == null)
			return "redirect:/login";

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
	public String updatePassword(@RequestParam("password") String password, Model model, RedirectAttributes redirect) {
		Account acc = (Account) session.getAttribute("account");
		if (acc == null)
			return "redirect:/login";

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

	// Upload avatar
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
