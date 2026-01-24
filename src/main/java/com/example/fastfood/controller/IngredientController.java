package com.example.fastfood.controller;

import com.example.fastfood.entity.Ingredient;
import com.example.fastfood.repository.IngredientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingredients") // API này khớp với Frontend
@CrossOrigin(origins = "*")
public class IngredientController {

    @Autowired
    private IngredientRepository ingredientRepository;

    // 1. API Lấy danh sách kho (Frontend đang gọi cái này mà bị lỗi)
    @GetMapping
    public List<Ingredient> getAllIngredients() {
        return ingredientRepository.findAll();
    }

    // 2. API Thêm nguyên liệu mới (Nút "Thêm nguyên liệu" cần cái này)
    @PostMapping
    public Ingredient createIngredient(@RequestBody Ingredient ingredient) {
        return ingredientRepository.save(ingredient);
    }

    // 3. API Cập nhật nguyên liệu
    @PutMapping("/{id}")
    public ResponseEntity<?> updateIngredient(@PathVariable Long id, @RequestBody Ingredient details) {
        Ingredient ingredient = ingredientRepository.findById(id).orElseThrow();
        ingredient.setName(details.getName());
        ingredient.setUnit(details.getUnit());
        ingredient.setQuantity(details.getQuantity());
        ingredient.setMinLimit(details.getMinLimit()); // Cập nhật cả mức cảnh báo
        return ResponseEntity.ok(ingredientRepository.save(ingredient));
    }

    // 4. API Xóa nguyên liệu
    @DeleteMapping("/{id}")
    public void deleteIngredient(@PathVariable Long id) {
        ingredientRepository.deleteById(id);
    }
}