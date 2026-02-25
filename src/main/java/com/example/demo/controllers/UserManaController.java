package com.example.demo.controllers;

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/user-mana")
public class UserManaController {

	@Autowired
	private AccountRepository repo;

	// ✅ Hiển thị danh sách user
	@GetMapping
	public String index(Model model) {
		model.addAttribute("accounts", repo.findAll());
		model.addAttribute("account", new Account());
		return "admin/usermana";
	}

	@PostMapping("/update/{id}")
	public String update(@PathVariable("id") Integer id, @ModelAttribute Account formAcc,
			@RequestParam("file") MultipartFile file) {
		try {
			Account acc = repo.findById(id).orElse(null);
			if (acc == null)
				return "redirect:/user-mana";

			// Nếu có file ảnh mới
			if (!file.isEmpty()) {
				String fileName = file.getOriginalFilename();
				String uploadDir = new File("src/main/resources/static/images/").getAbsolutePath();
				Files.createDirectories(Paths.get(uploadDir));
				Path filePath = Paths.get(uploadDir, fileName);
				Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
				acc.setPhoto("/images/" + fileName);
			}

			// Cập nhật các thông tin khác
			acc.setFullName(formAcc.getFullName());
			acc.setEmail(formAcc.getEmail());
			acc.setPhone(formAcc.getPhone());

			repo.save(acc);
			return "redirect:/user-mana";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// ✅ Khóa user (trừ admin)
	@GetMapping("/lock/{id}")
	public String lock(@PathVariable("id") Integer id) {
		repo.findById(id).ifPresent(a -> {
			if (!a.getRole()) { // chỉ khóa nếu không phải admin
				a.setActived(false);
				repo.save(a);
			}
		});
		return "redirect:/user-mana";
	}

	// ✅ Mở khóa user (trừ admin)
	@GetMapping("/unlock/{id}")
	public String unlock(@PathVariable("id") Integer id) {
		repo.findById(id).ifPresent(a -> {
			if (!a.getRole()) { // chỉ mở nếu không phải admin
				a.setActived(true);
				repo.save(a);
			}
		});
		return "redirect:/user-mana";
	}
}
