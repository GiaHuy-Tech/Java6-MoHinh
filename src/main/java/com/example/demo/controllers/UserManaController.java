package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;

@Controller
@RequestMapping("/user-mana")
public class UserManaController {

    @Autowired
    private AccountRepository repo;

    private final String UPLOAD_DIR = "src/main/resources/static/images/";

    @GetMapping
    public String index(Model model) {
        model.addAttribute("accounts", repo.findAll());
        model.addAttribute("account", new Account());
        return "admin/usermana";
    }

    @PostMapping("/update/{id}")
    public String update(@PathVariable Integer id,
                         @ModelAttribute Account formAcc,
                         @RequestParam("file") MultipartFile file) {

        try {
            Optional<Account> optional = repo.findById(id);
            if (optional.isEmpty()) {
                return "redirect:/user-mana?error=NotFound";
            }

            Account acc = optional.get();

            // Upload ảnh mới
            if (!file.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path uploadPath = Paths.get(UPLOAD_DIR);

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filePath = uploadPath.resolve(fileName);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                acc.setAvatar("/images/" + fileName);
            }

            // Update thông tin
            acc.setFullName(formAcc.getFullName());
            acc.setEmail(formAcc.getEmail());
            acc.setPhone(formAcc.getPhone());
            acc.setGender(formAcc.getGender());
            acc.setBirthDay(formAcc.getBirthDay());
            acc.setRole(formAcc.getRole());

            repo.save(acc);

            return "redirect:/user-mana?success=Updated";

        } catch (IOException e) {
            return "redirect:/user-mana?error=UploadFailed";
        } catch (Exception e) {
            return "redirect:/user-mana?error=UpdateFailed";
        }
    }

    @GetMapping("/lock/{id}")
    public String lock(@PathVariable Integer id) {
        Account acc = repo.findById(id).orElse(null);

        if (acc != null && (acc.getRole() == null || !acc.getRole())) {
            acc.setActive(false);
            repo.save(acc);
        }
        return "redirect:/user-mana";
    }

    @GetMapping("/unlock/{id}")
    public String unlock(@PathVariable Integer id) {
        Account acc = repo.findById(id).orElse(null);

        if (acc != null) {
            acc.setActive(true);
            repo.save(acc);
        }
        return "redirect:/user-mana";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        try {
            repo.deleteById(id);
        } catch (Exception e) {
            return "redirect:/user-mana?error=CannotDelete";
        }
        return "redirect:/user-mana";
    }
}