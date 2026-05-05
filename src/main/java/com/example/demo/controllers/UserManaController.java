package com.example.demo.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Account;
import com.example.demo.model.Membership;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.MembershipRepository;

@Controller
@RequestMapping("/user-mana")
public class UserManaController {

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private MembershipRepository membershipRepo;

    private final String UPLOAD_DIR = "src/main/resources/static/images/";

    // ==============================
    // LOAD PAGE
    // ==============================
    @GetMapping
    public String index(Model model) {

        List<Account> accounts = accountRepo.findAll();

        model.addAttribute("accounts", accounts);
        model.addAttribute("memberships", membershipRepo.findAll());
        model.addAttribute("account", new Account());

        return "admin/usermana";
    }

    // ==============================
    // UPDATE USER
    // ==============================
    @PostMapping("/update/{id}")
    public String update(@PathVariable Integer id,
                         @ModelAttribute Account formAcc,
                         @RequestParam("file") MultipartFile file,
                         @RequestParam(value = "membershipId", required = false) Integer membershipId) {

        try {
            Optional<Account> optional = accountRepo.findById(id);
            if (optional.isEmpty()) {
                return "redirect:/user-mana?error=NotFound";
            }

            Account acc = optional.get();

            // ===== Upload avatar =====
            if (!file.isEmpty()) {
                String fileName = System.currentTimeMillis()
                        + "_" + file.getOriginalFilename();

                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filePath = uploadPath.resolve(fileName);
                Files.copy(file.getInputStream(),
                        filePath,
                        StandardCopyOption.REPLACE_EXISTING);

                acc.setAvatar("/images/" + fileName);
            }

            // ===== Update basic info =====
            acc.setFullName(formAcc.getFullName());
            acc.setEmail(formAcc.getEmail());
            acc.setPhone(formAcc.getPhone());
            acc.setGender(formAcc.getGender());
            acc.setBirthDay(formAcc.getBirthDay());
            acc.setRole(formAcc.getRole());
            acc.setActive(formAcc.getActive());

            // ===== Update Membership =====
            if (membershipId != null) {
                Membership membership = membershipRepo
                        .findById(membershipId)
                        .orElse(null);
                acc.setMembership(membership);
            }

            accountRepo.save(acc);

            return "redirect:/user-mana?success=Updated";

        } catch (IOException e) {
            return "redirect:/user-mana?error=UploadFailed";
        } catch (Exception e) {
            return "redirect:/user-mana?error=UpdateFailed";
        }
    }

    // ==============================
    // LOCK USER
    // ==============================
    @GetMapping("/lock/{id}")
    public String lock(@PathVariable Integer id) {

        Account acc = accountRepo.findById(id).orElse(null);

        if (acc != null && (acc.getRole() == null || !acc.getRole())) {
            acc.setActive(false);
            accountRepo.save(acc);
        }

        return "redirect:/user-mana";
    }

    // ==============================
    // UNLOCK USER
    // ==============================
    @GetMapping("/unlock/{id}")
    public String unlock(@PathVariable Integer id) {

        Account acc = accountRepo.findById(id).orElse(null);

        if (acc != null) {
            acc.setActive(true);
            accountRepo.save(acc);
        }

        return "redirect:/user-mana";
    }

    // ==============================
    // DELETE USER
    // ==============================
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {

        try {
            accountRepo.deleteById(id);
        } catch (Exception e) {
            return "redirect:/user-mana?error=CannotDelete";
        }

        return "redirect:/user-mana";
    }
}