package com.jimmy.monetization.monetizationbackend.catalog;

import com.jimmy.monetization.monetizationbackend.catalog.dto.CreateProductRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class ProductAdminController {

    private final ProductService productService;

    public ProductAdminController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody CreateProductRequest req) {
        try {
            Product p = productService.create(req);
            return ResponseEntity.status(201).body(p);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
