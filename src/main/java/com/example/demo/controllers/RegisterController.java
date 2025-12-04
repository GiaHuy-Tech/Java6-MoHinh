	package com.example.demo.controllers;
	
	import java.net.http.HttpClient.Redirect;
	
	import org.springframework.beans.factory.annotation.Autowired;
	import org.springframework.stereotype.Controller;
	import org.springframework.ui.Model;
	import org.springframework.validation.BindingResult;
	import org.springframework.web.bind.annotation.GetMapping;
	import org.springframework.web.bind.annotation.ModelAttribute;
	import org.springframework.web.bind.annotation.PostMapping;
	
	import com.example.demo.model.Account;
	import com.example.demo.repository.AccountRepository;
	
	import jakarta.validation.Valid;
	
	@Controller
	public class RegisterController {
	
	    @Autowired
	    private AccountRepository accountRepo;
	
	    // Hiển thị form đăng ký
	    @GetMapping("/register")
	    public String showRegisterForm(Model model) {
	        model.addAttribute("account", new Account());
	        return "client/register";
	    }
	
	    // Xử lý khi nhấn nút Đăng ký
	    @PostMapping("/register")
	    public String processRegister(
	            @Valid @ModelAttribute("account") Account account,
	            BindingResult result,
	            Model model) {
	
	        System.out.println("==> Dữ liệu nhận được từ form: " + account);
	
	        // ✅ Kiểm tra email trùng
	        try {
	            if (accountRepo.existsByEmail(account.getEmail())) {
	                result.rejectValue("email", "error.account", "Email này đã được sử dụng");
	            }
	        } catch (Exception e) {
	            System.out.println("==> Lỗi khi kiểm tra email: " + e.getMessage());
	        }
	
	        // ✅ Kiểm tra phone trùng
	        try {
	            if (accountRepo.existsByPhone(account.getPhone())) {
	                result.rejectValue("phone", "error.account", "Số điện thoại đã tồn tại");
	            }
	        } catch (Exception e) {
	            System.out.println("==> Lỗi khi kiểm tra phone: " + e.getMessage());
	        }
	
	        // Nếu có lỗi validation → quay lại form
	        if (result.hasErrors()) {
	            System.out.println("==> Có lỗi validate: " + result.getAllErrors());
	            return "client/register";
	        }
	
	        // ✅ Gán giá trị mặc định
	        account.setActived(true);
	        account.setRole(false);
	
	        if (account.getPhoto() == null || account.getPhoto().isEmpty()) {
	            account.setPhoto("default.jpg");
	        }
	
	        try {
	            accountRepo.save(account);
	            System.out.println("==> Đã lưu tài khoản: " + account.getEmail());
	        } catch (Exception e) {
	            System.out.println("==> Lỗi khi lưu account: " + e.getMessage());
	            model.addAttribute("error", "Không thể lưu tài khoản vào cơ sở dữ liệu!");
	            return "client/register";
	        }
	
	        // ✅ Thông báo thành công
	        model.addAttribute("message", "Đăng ký thành công!");
	        model.addAttribute("account", new Account()); // reset form sau khi đăng ký
	        return "redirect:/login"; // <— Dòng đúng
	    }
	}
