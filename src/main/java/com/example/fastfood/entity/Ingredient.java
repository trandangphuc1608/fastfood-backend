package com.example.fastfood.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ingredients")
@Getter
@Setter
@NoArgsConstructor // Bắt buộc cho JPA
public class Ingredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String unit;
    private Double quantity;
    private Double minLimit; // Mức cảnh báo khi sắp hết hàng

    // Constructor có tham số (Để DataSeeder chạy được)
    public Ingredient(String name, String unit, Double quantity, Double minLimit) {
        this.name = name;
        this.unit = unit;
        this.quantity = quantity;
        this.minLimit = minLimit;
    }
}