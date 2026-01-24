package com.example.fastfood.controller;

import com.example.fastfood.entity.*;
import com.example.fastfood.repository.*;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductIngredientRepository productIngredientRepository; // Để lấy công thức món ăn

    @Autowired
    private IngredientRepository ingredientRepository; // Để trừ kho nguyên liệu

    // 1. API Lấy tất cả đơn hàng (Admin/Bếp/Thu ngân) - Mới nhất lên đầu
    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    // 2. API Tạo đơn hàng mới (Khách đặt)
    @PostMapping
    @Transactional
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        Order order = new Order();

        // Xử lý thông tin khách hàng (nếu đã đăng nhập)
        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId()).orElse(null);
            order.setUser(user);
            
            // LOGIC AN TOÀN: Nếu có User mà không có tên khách, tự lấy tên User điền vào
            if (user != null && (request.getCustomerName() == null || request.getCustomerName().isEmpty())) {
                order.setCustomerName(user.getFullName()); 
            }
        } else {
            // Nếu không có User ID, lấy tên khách vãng lai từ request
            order.setCustomerName(request.getCustomerName());
        }
        // ---------------------------

        order.setPhone(request.getPhone());
        order.setAddress(request.getAddress());
        
        order.setOrderDate(new Date());
        order.setStatus("PENDING");

        // Lưu tạm đơn hàng để sinh ID
        Order savedOrder = orderRepository.save(order);

        List<OrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // Xử lý từng món ăn trong giỏ
        for (CartItem itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn ID: " + itemReq.getProductId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setPrice(product.getPrice()); // Lưu giá tại thời điểm mua

            // Lưu chi tiết đơn hàng
            orderItemRepository.save(orderItem);
            items.add(orderItem);

            // Cộng dồn tổng tiền
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }

        savedOrder.setItems(items);
        savedOrder.setTotalAmount(totalAmount);

        // Lưu lại lần cuối với tổng tiền chuẩn xác
        return ResponseEntity.ok(orderRepository.save(savedOrder));
    }

    // 3. API Cập nhật trạng thái & TRỪ KHO (Admin/Bếp duyệt đơn)
    @PutMapping("/{id}/status")
    @Transactional
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newStatus = payload.get("status"); // Lấy status từ JSON { "status": "..." }
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        String oldStatus = order.getStatus();

        // --- LOGIC TRỪ KHO: Chỉ chạy khi chuyển từ PENDING -> PROCESSING ---
        if ("PENDING".equals(oldStatus) && ("PROCESSING".equals(newStatus) || "PAID".equals(newStatus))) {
            
            // Duyệt qua từng món trong đơn
            for (OrderItem orderItem : order.getItems()) {
                Product product = orderItem.getProduct();
                int quantityOrdered = orderItem.getQuantity();

                // Lấy công thức (recipe) của món đó
                List<ProductIngredient> recipe = productIngredientRepository.findByProductId(product.getId());

                // Trừ từng nguyên liệu trong kho
                for (ProductIngredient pi : recipe) {
                    Ingredient warehouseItem = pi.getIngredient();
                    
                    // Tính lượng cần: (Định lượng 1 món) * (Số lượng khách đặt)
                    double totalNeeded = pi.getQuantityNeeded() * quantityOrdered;

                    // Kiểm tra tồn kho
                    if (warehouseItem.getQuantity() < totalNeeded) {
                        // Nếu thiếu thì báo lỗi ngay, không cho chuyển trạng thái
                        return ResponseEntity.badRequest().body(
                            "Kho không đủ nguyên liệu: " + warehouseItem.getName() + 
                            ". Cần: " + totalNeeded + " " + warehouseItem.getUnit() + 
                            ", Còn: " + warehouseItem.getQuantity()
                        );
                    }

                    // Trừ kho và lưu lại
                    warehouseItem.setQuantity(warehouseItem.getQuantity() - totalNeeded);
                    ingredientRepository.save(warehouseItem);
                }
            }
        }

        order.setStatus(newStatus);
        return ResponseEntity.ok(orderRepository.save(order));
    }

    // 4. API Lịch sử đơn hàng của khách (Theo User ID)
    @GetMapping("/my-orders/{userId}")
    public List<Order> getMyOrders(@PathVariable Long userId) {
        // Đảm bảo OrderRepository có hàm findByUserIdOrderByOrderDateDesc
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
    }

    // 5. API Thống kê tổng quan (3 ô số trên Dashboard)
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long pending = orderRepository.countByStatus("PENDING");
        long completed = orderRepository.countByStatus("COMPLETED");
        
        BigDecimal revenue = orderRepository.sumTotalAmountByStatus("COMPLETED");
        if (revenue == null) revenue = BigDecimal.ZERO;

        Map<String, Object> response = new HashMap<>();
        response.put("pendingOrders", pending);
        response.put("completedOrders", completed);
        response.put("totalRevenue", revenue);
        
        return ResponseEntity.ok(response);
    }

    // 6. API Biểu đồ doanh thu 7 ngày gần nhất (ĐÃ SỬA ĐỂ KHỚP FRONTEND)
    @GetMapping("/revenue-chart")
    public ResponseEntity<List<Map<String, Object>>> getRevenueChart() {
        // 1. Lấy dữ liệu thô (Object[]) từ Repo để tránh lỗi định dạng Map của DB
        List<Object[]> data = orderRepository.getRevenueLast7Days();
        
        // 2. Tự chuyển đổi sang Map với key chữ thường ("date", "revenue")
        List<Map<String, Object>> result = new ArrayList<>();
        if (data != null) {
            for (Object[] row : data) {
                Map<String, Object> map = new HashMap<>();
                map.put("date", row[0]);     // Ngày
                map.put("revenue", row[1]);  // Doanh thu
                result.add(map);
            }
        }
        
        // Frontend sẽ nhận được: [{"date": "22/01", "revenue": 500000}, ...] -> Biểu đồ vẽ chuẩn ngay
        return ResponseEntity.ok(result);
    }

    // --- DTO CLASSES (Dùng để nhận dữ liệu từ Frontend) ---
    @Getter @Setter
    public static class OrderRequest {
        private Long userId;
        private String customerName;
        private String phone;
        private String address;
        private List<CartItem> items;
    }

    @Getter @Setter
    public static class CartItem {
        private Long productId;
        private int quantity;
    }
}