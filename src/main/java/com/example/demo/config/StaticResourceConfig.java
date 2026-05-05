package com.example.demo.config;

import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Lấy chính xác đường dẫn tuyệt đối (Absolute Path) từ ổ cứng máy tính
        String avatarPath = Paths.get("uploads/avatar").toAbsolutePath().toUri().toString();
        String categoriesPath = Paths.get("uploads/categories").toAbsolutePath().toUri().toString();
        String productsPath = Paths.get("uploads/products").toAbsolutePath().toUri().toString();

        registry.addResourceHandler("/images/avatar/**")
                .addResourceLocations(avatarPath);

        registry.addResourceHandler("/images/categories/**")
                .addResourceLocations(categoriesPath);

        registry.addResourceHandler("/images/products/**")
                .addResourceLocations(productsPath);
    }
}