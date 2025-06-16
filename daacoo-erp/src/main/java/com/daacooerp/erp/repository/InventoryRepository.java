package com.daacooerp.erp.repository;

import com.daacooerp.erp.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    // 根据产品名称查询
    Page<Inventory> findByProductNameContaining(String productName, Pageable pageable);

    // 根据产品编码查询
    Page<Inventory> findByProductCodeContaining(String productCode, Pageable pageable);

    // 根据类别查询
    Page<Inventory> findByCategoryContaining(String category, Pageable pageable);

    // 根据商品名称精确查找
    java.util.Optional<Inventory> findByProductName(String productName);

    // 获取所有商品名称（去重）
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT i.productName FROM Inventory i")
    java.util.List<String> findAllDistinctProductNames();
}