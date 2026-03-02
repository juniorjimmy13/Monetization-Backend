package com.jimmy.monetization.monetizationbackend.catalog;

import com.jimmy.monetization.monetizationbackend.catalog.dto.CreateProductRequest;
import com.jimmy.monetization.monetizationbackend.catalog.dto.UpdateProductRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
    // LIST
    @GetMapping("/products")
    public ResponseEntity<?> listProducts() {
        return ResponseEntity.ok(productService.listForTenant());
    }

    // READ by SKU
    @GetMapping("/products/sku/{sku}")
    public ResponseEntity<?> getBySku(@PathVariable String sku) {
        try {
            return ResponseEntity.ok(productService.getBySku(sku));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // UPDATE by SKU
    @PutMapping("/products/sku/{sku}")
    public ResponseEntity<?> updateBySku(@PathVariable String sku, @RequestBody UpdateProductRequest req) {
        try {
            return ResponseEntity.ok(productService.updateBySku(sku, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE by SKU
    @DeleteMapping("/products/sku/{sku}")
    public ResponseEntity<?> deleteBySku(@PathVariable String sku) {
        try {
            productService.deleteBySku(sku);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

}
