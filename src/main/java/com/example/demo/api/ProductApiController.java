package com.example.demo.api;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Products;
import com.example.demo.repository.ProductRepository;

@RestController
@RequestMapping(value = "/api/products", produces = "application/json")
public class ProductApiController {

    @Autowired
    private ProductRepository productRepo;

    @GetMapping
    public ResponseEntity<List<Products>> getAll() {
        return ResponseEntity.ok(productRepo.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Products> getById(@PathVariable Integer id) {
        return productRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Products> create(@RequestBody Products product) {
        product.setId(null);
        product.setCreatedDate(new Date());
        return ResponseEntity.status(201).body(productRepo.save(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Products> update(@PathVariable Integer id,
                                           @RequestBody Products product) {
        Products existing = productRepo.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        product.setId(id);
        product.setCreatedDate(existing.getCreatedDate());
        return ResponseEntity.ok(productRepo.save(product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!productRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}