package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import com.example.demo.service.AccountService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AccountService userService;
    private final CustomSuccessHandler successHandler;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(AccountService userService,
                          CustomSuccessHandler successHandler,
                          CustomAccessDeniedHandler accessDeniedHandler) {
        this.userService = userService;
        this.successHandler = successHandler;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance(); // Chỉ dùng dev
    }

    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationFailureHandler authFailureHandler() {
        return new AuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request,
                                                HttpServletResponse response,
                                                org.springframework.security.core.AuthenticationException exception)
                    throws IOException, ServletException {
                String redirectUrl = "/login?error";
                if (exception instanceof org.springframework.security.authentication.LockedException) {
                    redirectUrl = "/login?locked";
                } else if (exception instanceof org.springframework.security.core.userdetails.UsernameNotFoundException) {
                    redirectUrl = "/login?notfound";
                }
                response.sendRedirect(redirectUrl);
            }
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Public
                        .requestMatchers("/", "/login", "/register", "/forgot",
                                "/css/**", "/js/**", "/images/**", "/uploads/**", "/vendor/**").permitAll()

                        // USER & ADMIN can access user pages
                        .requestMatchers("/account/**", "/cart/**", "/checkout/**").hasAnyRole("USER", "ADMIN")

                        // ADMIN only
                        .requestMatchers("/stats/**",
                                "/product-mana/**",
                                "/admin/cart/**",
                                "/user-mana/**",
                                "/cata-mana/**").hasRole("ADMIN")

                        // Other pages require login
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(authFailureHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler))
                .build();
    }
}
