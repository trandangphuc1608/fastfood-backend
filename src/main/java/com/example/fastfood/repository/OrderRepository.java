package com.example.fastfood.repository;

import com.example.fastfood.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // Import cái này
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 1. Lấy lịch sử đơn hàng của khách (Sắp xếp mới nhất lên đầu)
    // Lưu ý: Trong Entity Order phải có thuộc tính 'orderDate'
    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);

    // 2. Đếm số lượng đơn theo trạng thái (Dùng cho thống kê PENDING, COMPLETED...)
    long countByStatus(String status);

    // 3. Tính tổng doanh thu theo trạng thái (Chỉ tính đơn COMPLETED)
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") String status);

    // 4. Lấy dữ liệu biểu đồ 7 ngày gần nhất
    // Lưu ý: Sửa 'created_at' thành 'order_date' để khớp với Entity
    @Query(value = """
        SELECT 
            DATE_FORMAT(order_date, '%d/%m') as date, 
            SUM(total_amount) as revenue 
        FROM orders 
        WHERE status = 'COMPLETED' 
        AND order_date >= DATE(NOW()) - INTERVAL 7 DAY
        GROUP BY DATE(order_date)
        ORDER BY DATE(order_date) ASC
    """, nativeQuery = true)
    List<Object[]> getRevenueLast7Days();
}