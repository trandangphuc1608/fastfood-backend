package com.example.fastfood.controller;

import com.example.fastfood.entity.Product;
import com.example.fastfood.entity.ProductIngredient;
import com.example.fastfood.entity.Ingredient;
import com.example.fastfood.repository.ProductRepository;
import com.example.fastfood.repository.ProductIngredientRepository;
import com.example.fastfood.repository.IngredientRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductIngredientRepository productIngredientRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    // Đường dẫn lưu ảnh
    private final String UPLOAD_DIR = "uploads/";

    // 1. Lấy danh sách món ăn
    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // 2. Tạo món ăn mới
    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }

    // 3. Lấy chi tiết món ăn
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productRepository.findById(id).orElse(null);
    }

    // 4. Cập nhật món ăn
    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        Product product = productRepository.findById(id).orElseThrow();
        
        product.setName(productDetails.getName());
        product.setPrice(productDetails.getPrice());
        product.setDescription(productDetails.getDescription());
        product.setImageUrl(productDetails.getImageUrl());
        product.setCategory(productDetails.getCategory());
        product.setIsAvailable(productDetails.getIsAvailable());

        return productRepository.save(product);
    }

    // 5. Xóa món ăn
    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productRepository.deleteById(id);
    }

    // --- API UPLOAD ẢNH ---
    @PostMapping("/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/" + fileName; 
    }

    // --- 👇 CÁC API VỀ CÔNG THỨC (QUAN TRỌNG ĐỂ THÊM NGUYÊN LIỆU) 👇 ---

    // 6. Lấy danh sách nguyên liệu của món
    @GetMapping("/{id}/ingredients")
    public List<ProductIngredient> getIngredients(@PathVariable Long id) {
        return productIngredientRepository.findByProductId(id);
    }

    // 7. API THÊM/SỬA 1 NGUYÊN LIỆU VÀO MÓN (Khớp với nút "Thêm" ở Frontend)
    @PostMapping("/{productId}/ingredients")
    public ResponseEntity<?> addIngredientToProduct(
            @PathVariable Long productId, 
            @RequestBody Map<String, Object> payload) {
        
        try {
            Long ingredientId = Long.valueOf(payload.get("ingredientId").toString());
            Double quantity = Double.valueOf(payload.get("quantity").toString());

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));
            
            Ingredient ingredient = ingredientRepository.findById(ingredientId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nguyên liệu"));

            // Kiểm tra: Nếu món này đã có nguyên liệu đó rồi thì cập nhật số lượng, chưa có thì tạo mới
            ProductIngredient pi = productIngredientRepository.findByProductId(productId).stream()
                    .filter(item -> item.getIngredient().getId().equals(ingredientId))
                    .findFirst()
                    .orElse(new ProductIngredient());

            pi.setProduct(product);
            pi.setIngredient(ingredient);
            pi.setQuantityNeeded(quantity);

            productIngredientRepository.save(pi);
            return ResponseEntity.ok("Đã cập nhật nguyên liệu thành công!");
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi thêm nguyên liệu: " + e.getMessage());
        }
    }

    // 8. API XÓA 1 NGUYÊN LIỆU KHỎI MÓN (Khớp với nút "Xóa" thùng rác ở Frontend)
    @DeleteMapping("/{productId}/ingredients/{ingredientId}")
    public ResponseEntity<?> removeIngredientFromProduct(
            @PathVariable Long productId, 
            @PathVariable Long ingredientId) {
        
        List<ProductIngredient> list = productIngredientRepository.findByProductId(productId);
        boolean removed = false;
        
        for (ProductIngredient pi : list) {
            if (pi.getIngredient().getId().equals(ingredientId)) {
                productIngredientRepository.delete(pi);
                removed = true;
                break;
            }
        }
        
        if (removed) {
            return ResponseEntity.ok("Đã xóa nguyên liệu khỏi công thức");
        } else {
            return ResponseEntity.badRequest().body("Nguyên liệu không tồn tại trong món này");
        }
    }
}