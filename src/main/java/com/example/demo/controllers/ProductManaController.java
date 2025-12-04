package com.example.demo.controllers;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Products;
import com.example.demo.model.Category;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.CategoryRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/product-mana")
public class ProductManaController {

    @Autowired
    ProductRepository productRepo;

    @Autowired
    CategoryRepository categoryRepo;

    // ✅ Trang quản lý sản phẩm
    @GetMapping
    public String list(Model model) {
        model.addAttribute("list", productRepo.findAll());
        model.addAttribute("product", new Products());
        model.addAttribute("categories", categoryRepo.findAll());
        return "admin/productMana";
    }

    // ✅ Thêm sản phẩm
    @PostMapping("/add")
    public String add(@Valid @ModelAttribute("product") Products product,
                      BindingResult result,
                      @RequestParam("categoryId") Integer categoryId,
                      Model model) {
        if (result.hasErrors()) {
            model.addAttribute("list", productRepo.findAll());
            model.addAttribute("categories", categoryRepo.findAll());
            return "admin/productMana";
        }

        Category category = categoryRepo.findById(categoryId).orElse(null);
        product.setCategory(category);
        product.setCreatedDate(new Date());
        productRepo.save(product);
        return "redirect:/product-mana";
    }

    @GetMapping("/edit/{id}")
    public String editProduct(@PathVariable("id") Integer id, Model model) {
        Products product = productRepo.findById(id).orElse(null);
        List<Category> categories = categoryRepo.findAll();

        model.addAttribute("product", product);
        model.addAttribute("categories", categories);
        model.addAttribute("list", productRepo.findAll()); // để hiển thị lại danh sách
        return "admin/productMana";
    }


    // ✅ Cập nhật sản phẩm
    @PostMapping("/update")
    public String update(@Valid @ModelAttribute("product") Products product,
                         BindingResult result,
                         @RequestParam("categoryId") Integer categoryId,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("list", productRepo.findAll());
            model.addAttribute("categories", categoryRepo.findAll());
            return "admin/productMana";
        }

        Category category = categoryRepo.findById(categoryId).orElse(null);
        product.setCategory(category);
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
