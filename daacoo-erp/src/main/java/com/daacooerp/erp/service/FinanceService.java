package com.daacooerp.erp.service;

import com.daacooerp.erp.entity.FinanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface FinanceService {
    
    // 获取指定日期范围内的财务记录
    List<FinanceRecord> getFinanceRecords(Date startDate, Date endDate);
    
    // 分页获取财务记录
    Page<FinanceRecord> getFinanceRecordsPaged(Date startDate, Date endDate, Pageable pageable);
    
    // 获取财务统计数据
    Map<String, Object> getFinanceStatistics(Date startDate, Date endDate);
    
    // 创建财务记录
    FinanceRecord createFinanceRecord(FinanceRecord record);
    
    // 更新财务记录
    FinanceRecord updateFinanceRecord(Long id, FinanceRecord record);
    
    // 删除财务记录
    void deleteFinanceRecord(Long id);
    
    // 获取指定年份的月度财务数据
    Map<String, Object> getMonthlyFinanceData(int year);
} 