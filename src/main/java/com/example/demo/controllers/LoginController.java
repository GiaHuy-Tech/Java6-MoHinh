package com.example.demo.controllers;

import java.util.Optional;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class LoginController {

	@Autowired
	private AccountRepository accountRepo;

	@Autowired
	private HttpSession session;

	// Trang login — tự động lấy cookie (nếu có)
	@GetMapping("/login")
	public String showLoginForm(HttpServletRequest request, Model model) {
		Account acc = new Account();
		// Lấy cookie email và password nếu tồn tại
		String email = getCookieValue(request, "email");
		String password = getCookieValue(request, "password");

		if (email != null && password != null) {
			acc.setEmail(email);
			acc.setPassword(password);
		}

		// Nếu có cả email và password trong cookie thì coi như đã chọn "remember"
		boolean remember = (email != null && password != null);
		
		// Gán vào model để Thymeleaf bind form
		model.addAttribute("account", acc);
		model.addAttribute("remember", remember);

		return "client/login";
	}
	
	// Xử lý login
	@PostMapping("/login")
	public String processLogin(
			 @Valid @ModelAttribute("account") Account account, BindingResult result,
			@RequestParam(value = "remember", required = false) String remember, Model model,
			HttpServletResponse response) {
		
		// Nếu có lỗi validate từ entity (email trống, mật khẩu ngắn...) 
		if (result.hasErrors()) { 
			return "client/login";
		}
		
		Optional<Account> optionalAccount = accountRepo.findByEmail(account.getEmail());

		if (optionalAccount.isEmpty()) {
			model.addAttribute("errorMessage", "Tài khoản không tồn tại!");
			return "client/login";
		}

		Account dbAccount = optionalAccount.get();

		if (!account.getPassword().equals(dbAccount.getPassword())) { 
			model.addAttribute("errorMessage", "Mật khẩu không đúng!"); 
			return "client/login"; 
		}

		// Đăng nhập thành công
		session.setAttribute("account", dbAccount);

		// Nếu chọn “Ghi nhớ đăng nhập” thì lưu cookie 7 ngày
		if (remember != null) {
			saveCookie(response, "email", account.getEmail(), 7);
			saveCookie(response, "password", account.getPassword(), 7);
		} else {
			// Nếu không tick thì xóa cookie cũ
			clearCookie(response, "email");
clearCookie(response, "password");
		}

		return "client/login";
	}

	// Logout — xóa session + cookie
	@GetMapping("/logout")
	public String logout(HttpSession session, HttpServletResponse response) {
		session.invalidate();
		clearCookie(response, "email");
		clearCookie(response, "password");
		return "redirect:/login";
	}

	private void saveCookie(HttpServletResponse response, String name, String value, int days) {
		Cookie cookie = new Cookie(name, value);
		cookie.setMaxAge(days * 24 * 60 * 60); // thời hạn tính bằng giây
		cookie.setPath("/");
		response.addCookie(cookie);
	}

	private void clearCookie(HttpServletResponse response, String name) {
		Cookie cookie = new Cookie(name, null);
		cookie.setMaxAge(0);
		cookie.setPath("/");
		response.addCookie(cookie);
	}

	private String getCookieValue(HttpServletRequest request, String name) {
		if (request.getCookies() == null)
			return null;
		return Arrays.stream(request.getCookies()).filter(c -> c.getName().equals(name)).map(Cookie::getValue)
				.findFirst().orElse(null);
	}
}