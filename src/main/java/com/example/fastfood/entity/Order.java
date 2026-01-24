package com.example.fastfood.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.Date; // Nhớ import Date
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;

    private BigDecimal totalAmount;

    private String status; // PENDING, PROCESSING, COMPLETED, CANCELLED

    // --- 👇 CÁC TRƯỜNG MỚI BẠN CẦN THÊM VÀO 👇 ---
    
    private String customerName; // Tên khách vãng lai
    
    private String phone;        // SĐT giao hàng
    
    private String address;      // Địa chỉ giao hàng
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date orderDate;      // Ngày giờ đặt hàng
    
    // ----------------------------------------------
}