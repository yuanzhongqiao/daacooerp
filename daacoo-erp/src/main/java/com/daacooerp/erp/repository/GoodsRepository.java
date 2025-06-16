package com.daacooerp.erp.repository;

import com.daacooerp.erp.entity.Goods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsRepository extends JpaRepository<Goods, Long> {
    List<Goods> findByName(String name);
    
    List<Goods> findByCode(String code);
} 