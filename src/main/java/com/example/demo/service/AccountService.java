package com.example.demo.service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;         // Đã bổ sung
import org.springframework.data.domain.Pageable;     // Đã bổ sung
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Account;
import com.example.demo.repository.AccountRepository;

@Service
@Transactional
public class AccountService implements UserDetailsService {

    @Autowired
    private AccountRepository accountRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Account acc = accountRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + email));

        // 1. CHẶN TÀI KHOẢN GOOGLE (Password dài 36 ký tự)
        // Chúng ta dùng DisabledException để ném ra, AuthenticationFailureHandler sẽ bắt nó
        if (acc.getPassword() != null && acc.getPassword().length() == 36) {
            throw new DisabledException("GOOGLE_USER");
        }

        // 2. CHẶN TÀI KHOẢN BỊ KHÓA
        if (Boolean.FALSE.equals(acc.getActive())) {
            throw new LockedException("Tài khoản của bạn đã bị khóa!");
        }

        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(Boolean.TRUE.equals(acc.getRole()) ? "ROLE_ADMIN" : "ROLE_USER"));

        return User.builder()
                .username(acc.getEmail())
                .password(acc.getPassword())
                .authorities(authorities)
                .build();
    }

    // =====================================================
    // CRUD CƠ BẢN
    // =====================================================
    public Page<Account> getAll(Pageable pageable) { 
        return accountRepo.findAll(pageable); 
    }

    public Iterable<Account> findAll() { 
        return accountRepo.findAll(); 
    }

    public Optional<Account> findById(Integer id) { 
        return accountRepo.findById(id); 
    }

    public Optional<Account> findByEmail(String email) { 
        return accountRepo.findByEmail(email); 
    }

    public Account save(Account acc) { 
        return accountRepo.save(acc); 
    }

    public boolean existsByEmail(String email) { 
        return accountRepo.existsByEmail(email); 
    }

    public boolean existsByPhone(String phone) { 
        return accountRepo.existsByPhone(phone); 
    }

    public long count() { 
        return accountRepo.count(); 
    }

    // =====================================================
    // DELETE
    // =====================================================
    public boolean deleteById(Integer id) { 
        if (!accountRepo.existsById(id)) {
            return false;
        }
        accountRepo.deleteById(id);
        return true; 
    }

    // =====================================================
    // TOGGLE ACTIVE (KHÓA / MỞ KHÓA)
    // =====================================================
    public boolean toggleActive(Integer id) {
        Account acc = accountRepo.findById(id).orElse(null);
        if (acc == null) {
            return false;
        }
        acc.setActive(acc.getActive() == null ? true : !acc.getActive());
        accountRepo.save(acc);
        return true;
    }
}