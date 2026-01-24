package com.example.fastfood.entity;

import com.fasterxml.jackson.annotation.JsonIgnore; // Import dòng này
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "product_ingredients")
@Getter
@Setter
public class ProductIngredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    @JsonIgnore // <--- QUAN TRỌNG: Thêm dòng này để ngắt vòng lặp khi xem chi tiết món
    private Product product;

    @ManyToOne
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    private Double quantityNeeded; // Số lượng cần cho 1 món (ví dụ: 0.2 kg thịt)
}