package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Category;
import com.example.demo.repository.CategoryRepository;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository repo;

    public List<Category> findAll() {
        return repo.findAll();
    }

    public Category findById(Integer id) {
        return repo.findById(id).orElse(null);
    }

    public Category save(Category c) {
        return repo.save(c);
    }

    public void delete(Integer id) {
        repo.deleteById(id);
    }
}
