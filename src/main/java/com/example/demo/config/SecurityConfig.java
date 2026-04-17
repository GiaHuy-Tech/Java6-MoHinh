package com.example.demo.config;

import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AccountService userService;
    private final CustomSuccessHandler successHandler;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomOAuth2UserService oAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    public SecurityConfig(AccountService userService, CustomSuccessHandler successHandler,
                          CustomAccessDeniedHandler accessDeniedHandler, CustomOAuth2UserService oAuth2UserService,
                          OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.userService = userService;
        this.successHandler = successHandler;
        this.accessDeniedHandler = accessDeniedHandler;
        this.oAuth2UserService = oAuth2UserService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    PasswordEncoder passwordEncoder() { return NoOpPasswordEncoder.getInstance(); }

    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // HANDLER BẮT LỖI
    @Bean
    public AuthenticationFailureHandler authFailureHandler() {
        return (request, response, exception) -> {
            String redirectUrl = "/login?error";
            if (exception instanceof DisabledException && "GOOGLE_USER".equals(exception.getMessage())) {
                redirectUrl = "/login?google";
            } else if (exception instanceof LockedException) {
                redirectUrl = "/login?locked";
            } else if (exception instanceof UsernameNotFoundException) {
                redirectUrl = "/login?notfound";
            }
            response.sendRedirect(redirectUrl);
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(authFailureHandler()) // <--- GẮN HANDLER VÀO ĐÂY
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(u -> u.userService(oAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/login?logout").permitAll())
                .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler))
                .build();
    }
}