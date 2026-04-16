package com.example.demo.service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // =====================================================
    // LOAD USER CHO SPRING SECURITY
    // =====================================================
    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Account acc = accountRepo.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Không tìm thấy tài khoản với email: " + email));

        // 🔒 Chặn tài khoản bị khóa
        if (Boolean.FALSE.equals(acc.getActive())) {
            throw new LockedException("Tài khoản của bạn đã bị khóa!");
        }

        // 🎯 Phân quyền
        Set<GrantedAuthority> authorities = new HashSet<>();

        if (Boolean.TRUE.equals(acc.getRole())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

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

        Boolean currentStatus = acc.getActive();
        acc.setActive(currentStatus == null ? true : !currentStatus);

        accountRepo.save(acc);
        return true;
    }
}