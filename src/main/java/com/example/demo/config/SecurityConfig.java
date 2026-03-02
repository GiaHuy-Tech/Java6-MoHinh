package com.example.demo.config;

import java.io.IOException;

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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.demo.filter.JwtFilter;
import com.example.demo.service.AccountService;
import com.example.demo.service.CustomOAuth2UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AccountService userService;
    private final CustomSuccessHandler successHandler;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomOAuth2UserService oAuth2UserService;

    public SecurityConfig(AccountService userService,
                          CustomSuccessHandler successHandler,
                          CustomAccessDeniedHandler accessDeniedHandler,
                          CustomOAuth2UserService oAuth2UserService) {
        this.userService = userService;
        this.successHandler = successHandler;
        this.accessDeniedHandler = accessDeniedHandler;
        this.oAuth2UserService = oAuth2UserService;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // LƯU Ý: Chỉ dùng cho môi trường Dev. Production nên dùng BCryptPasswordEncoder
        return NoOpPasswordEncoder.getInstance(); 
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
                                                AuthenticationException exception)
                    throws IOException, ServletException {
                
                String redirectUrl = "/login?error";
                
                if (exception instanceof LockedException) {
                    redirectUrl = "/login?locked";
                } else if (exception instanceof UsernameNotFoundException) {
                    redirectUrl = "/login?notfound";
                }
                
                response.sendRedirect(redirectUrl);
            }
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 1. QUAN TRỌNG: Cho phép truy cập trang lỗi để tránh vòng lặp "Response already committed"
                        .requestMatchers("/error").permitAll()

                        // 2. Tài nguyên tĩnh (CSS, JS, Images...)
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**", "/vendor/**", "/assets/**", "/webjars/**").permitAll()

                        // 3. Các trang công khai (Home, Login, Register, API Search...)
                        .requestMatchers("/", "/home", "/login", "/register", "/forgot", "/api/products/search").permitAll()

                        // 4. VNPay CALLBACK
                        .requestMatchers("/checkout/vnpay-return").permitAll()

                        // 5. Các trang USER & ADMIN
                        .requestMatchers("/account/**", "/cart/**", "/checkout/**").hasAnyRole("USER", "ADMIN")

                        // 6. Các trang chỉ ADMIN
                        .requestMatchers("/stats/**",
                                         "/product-mana/**",
                                         "/orders-mana/**", // Đã thêm trang quản lý đơn hàng
                                         "/admin/vouchers/**", // Đã thêm trang voucher
                                         "/admin/cart/**",
                                         "/user-mana/**",
                                         "/cata-mana/**").hasRole("ADMIN")

                        // Còn lại bắt buộc đăng nhập
                        .anyRequest().authenticated()
                )

                // Thêm JwtFilter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                // Cấu hình Form Login
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(authFailureHandler())
                        .permitAll()
                )

                // Cấu hình OAuth2 (Google)
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService)
                        )
                        .defaultSuccessUrl("/", true) // redirect về trang chủ sau khi login GG
                )

                // Cấu hình Logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true) // Hủy session
                        .deleteCookies("JSESSIONID") // Xóa cookie
                        .permitAll()
                )

                // Xử lý lỗi 403 (Không có quyền truy cập)
                .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler))
                
                .build();
    }
}