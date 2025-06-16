package com.daacooerp.erp.repository;

import com.daacooerp.erp.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByOrderType(String orderType);
    
    Page<Order> findByOrderType(String orderType, Pageable pageable);

    // @Query("SELECT function('MONTH', o.createdAt) as month, COUNT(o) as orderCount, SUM(o.amount) as totalAmount " +
    //        "FROM Order o WHERE function('YEAR', o.createdAt) = :year AND o.orderType = 'SALE' " +
    //        "GROUP BY function('MONTH', o.createdAt)")
    // List<Map<String, Object>> getMonthlySalesOrderStatistics(@Param("year") int year);

    @Query("SELECT function('MONTH', o.createdAt) as month, o.orderType as orderType, COUNT(o) as orderCount, SUM(o.amount) as totalAmount " +
           "FROM Order o WHERE function('YEAR', o.createdAt) = :year AND o.orderType IN ('SALE', 'PURCHASE') " +
           "GROUP BY function('MONTH', o.createdAt), o.orderType")
    List<Map<String, Object>> getMonthlyOrderStatisticsByType(@Param("year") int year);
}