package com.example.demo.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Category;
import com.example.demo.repository.CategoryRepository;

@RestController
@RequestMapping("/api/categories")
public class CategoryApiController {

    @Autowired
    private CategoryRepository repo;

    @GetMapping
    public ResponseEntity<List<Category>> getAll() {
        return ResponseEntity.ok(repo.findAll());
    }

    @PostMapping
    public ResponseEntity<?> create(@ModelAttribute Category c) {
        return ResponseEntity.status(201).body(repo.save(c));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id,
                                    @ModelAttribute Category data) {

        Category c = repo.findById(id).orElse(null);
        if (c == null) {
			return ResponseEntity.notFound().build();
		}

        c.setName(data.getName());
        c.setImage(data.getImage());

        return ResponseEntity.ok(repo.save(c));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}