package com.daacooerp.erp.repository;

import com.daacooerp.erp.entity.FinanceRecord;
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
public interface FinanceRecordRepository extends JpaRepository<FinanceRecord, Long> {
    
    // 根据日期范围查询财务记录
    List<FinanceRecord> findByRecordDateBetweenOrderByRecordDateAsc(Date startDate, Date endDate);
    
    // 分页查询
    Page<FinanceRecord> findByRecordDateBetween(Date startDate, Date endDate, Pageable pageable);
    
    // 获取指定日期范围内的财务统计数据
    @Query("SELECT SUM(f.income) as totalIncome, SUM(f.expense) as totalExpense, " +
           "SUM(f.profit) as totalProfit, COUNT(f) as recordCount " +
           "FROM FinanceRecord f WHERE f.recordDate BETWEEN :startDate AND :endDate")
    Map<String, Object> getFinanceStatistics(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
} 