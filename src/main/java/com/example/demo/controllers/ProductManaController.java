package com.example.demo.controllers;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Category;
import com.example.demo.model.Products;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/product-mana")
public class ProductManaController {

    @Autowired
    ProductRepository productRepo;

    @Autowired
    CategoryRepository categoryRepo;

    // ✅ Hiển thị danh sách
    @GetMapping
    public String list(Model model) {
        model.addAttribute("list", productRepo.findAll());
        model.addAttribute("product", new Products()); // Object rỗng cho form thêm mới
        model.addAttribute("categories", categoryRepo.findAll());
        return "admin/productMana";
    }

    // ✅ Thêm sản phẩm
    @PostMapping("/add")
    public String add(@Valid @ModelAttribute("product") Products product,
                      BindingResult result,
                      Model model) {
        
        // Nếu có lỗi validate (tên trống, giá sai, chưa chọn danh mục...)
        if (result.hasErrors()) {
            // Load lại dữ liệu cần thiết cho trang để hiển thị lỗi
            model.addAttribute("list", productRepo.findAll());
            model.addAttribute("categories", categoryRepo.findAll());
            return "admin/productMana";
        }

        // Gán ngày tạo hiện tại
        product.setCreatedDate(new Date());
        productRepo.save(product);
        
        return "redirect:/product-mana";
    }

    // ✅ Load dữ liệu lên form để sửa
    @GetMapping("/edit/{id}")
    public String editProduct(@PathVariable("id") Integer id, Model model) {
        Products product = productRepo.findById(id).orElse(null);
        
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryRepo.findAll());
        model.addAttribute("list", productRepo.findAll());
        
        return "admin/productMana";
    }

    // ✅ Cập nhật sản phẩm
    @PostMapping("/update")
    public String update(@Valid @ModelAttribute("product") Products product,
                         BindingResult result,
                         Model model) {
        
        if (result.hasErrors()) {
            model.addAttribute("list", productRepo.findAll());
            model.addAttribute("categories", categoryRepo.findAll());
            return "admin/productMana";
        }

        // Lấy sản phẩm cũ để giữ lại ngày tạo (CreatedDate)
        Products existingProduct = productRepo.findById(product.getId()).orElse(null);
        if (existingProduct != null) {
            product.setCreatedDate(existingProduct.getCreatedDate());
        } else {
            product.setCreatedDate(new Date());
        }

        productRepo.save(product);
        return "redirect:/product-mana";
    }

    // ✅ Xóa sản phẩm
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") Integer id) {
        productRepo.deleteById(id);
        return "redirect:/product-mana";
    }
}