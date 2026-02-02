package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cấu hình phục vụ ảnh từ thư mục uploads nằm ngang hàng với src
        
        // 1. Avatar
        registry.addResourceHandler("/images/avatar/**")
                .addResourceLocations("file:uploads/avatar/");

        // 2. Danh mục (Category)
        registry.addResourceHandler("/images/categories/**")
                .addResourceLocations("file:uploads/categories/");

        // 3. Sản phẩm (Products) - QUAN TRỌNG: Controller phải lưu đúng vào đây
        registry.addResourceHandler("/images/products/**")
                .addResourceLocations("file:uploads/products/");
    }
}