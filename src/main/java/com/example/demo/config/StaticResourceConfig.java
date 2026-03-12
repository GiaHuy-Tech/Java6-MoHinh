package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Thêm ./ để chỉ định thư mục gốc project
        registry.addResourceHandler("/images/avatar/**")
                .addResourceLocations("file:./uploads/avatar/");

        registry.addResourceHandler("/images/categories/**")
                .addResourceLocations("file:./uploads/categories/");

        registry.addResourceHandler("/images/products/**")
                .addResourceLocations("file:./uploads/products/");
    }
}