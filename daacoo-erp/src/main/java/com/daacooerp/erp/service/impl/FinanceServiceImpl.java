package com.daacooerp.erp.service.impl;

import com.daacooerp.erp.entity.FinanceRecord;
import com.daacooerp.erp.repository.FinanceRecordRepository;
import com.daacooerp.erp.service.FinanceService;
import com.daacooerp.erp.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class FinanceServiceImpl implements FinanceService {

    @Autowired
    private FinanceRecordRepository financeRecordRepository;

    @Autowired
    @Lazy
    private OrderService orderService;
    
    @Override
    public List<FinanceRecord> getFinanceRecords(Date startDate, Date endDate) {
        return financeRecordRepository.findByRecordDateBetweenOrderByRecordDateAsc(startDate, endDate);
    }
    
    @Override
    public Page<FinanceRecord> getFinanceRecordsPaged(Date startDate, Date endDate, Pageable pageable) {
        return financeRecordRepository.findByRecordDateBetween(startDate, endDate, pageable);
    }
    
    @Override
    public Map<String, Object> getFinanceStatistics(Date startDate, Date endDate) {
        Map<String, Object> statistics = financeRecordRepository.getFinanceStatistics(startDate, endDate);
        
        // 如果没有数据，返回默认值
        if (statistics == null || statistics.isEmpty()) {
            statistics = new HashMap<>();
            statistics.put("totalIncome", BigDecimal.ZERO);
            statistics.put("totalExpense", BigDecimal.ZERO);
            statistics.put("totalProfit", BigDecimal.ZERO);
            statistics.put("recordCount", 0L);
        }
        
        return statistics;
    }
    
    @Override
    @Transactional
    public FinanceRecord createFinanceRecord(FinanceRecord record) {
        // 设置创建时间
        if (record.getCreatedAt() == null) {
            record.setCreatedAt(new Date());
        }
        record.setUpdatedAt(new Date());
        
        // 计算利润
        if (record.getIncome() != null && record.getExpense() != null) {
            record.setProfit(record.getIncome().subtract(record.getExpense()));
        }
        
        return financeRecordRepository.save(record);
    }
    
    @Override
    @Transactional
    public FinanceRecord updateFinanceRecord(Long id, FinanceRecord record) {
        FinanceRecord existingRecord = financeRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("财务记录不存在: " + id));
        
        // 更新字段
        existingRecord.setRecordDate(record.getRecordDate());
        existingRecord.setIncome(record.getIncome());
        existingRecord.setExpense(record.getExpense());
        existingRecord.setRecordType(record.getRecordType());
        existingRecord.setDescription(record.getDescription());
        existingRecord.setUpdatedAt(new Date());
        
        // 计算利润
        if (existingRecord.getIncome() != null && existingRecord.getExpense() != null) {
            existingRecord.setProfit(existingRecord.getIncome().subtract(existingRecord.getExpense()));
        }
        
        return financeRecordRepository.save(existingRecord);
    }
    
    @Override
    @Transactional
    public void deleteFinanceRecord(Long id) {
        financeRecordRepository.deleteById(id);
    }
    
    @Override
    public Map<String, Object> getMonthlyFinanceData(int year) {
        // 创建包含12个月数据的结果
        Map<String, Object> result = new HashMap<>();
        List<BigDecimal> incomeList = new ArrayList<>(12);
        List<BigDecimal> expenseList = new ArrayList<>(12);
        List<BigDecimal> profitList = new ArrayList<>(12);
        
        // 设置日期范围为整年
        Calendar startCal = Calendar.getInstance();
        startCal.set(year, Calendar.JANUARY, 1, 0, 0, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        
        Calendar endCal = Calendar.getInstance();
        endCal.set(year, Calendar.DECEMBER, 31, 23, 59, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        
        // 获取全年数据
        List<FinanceRecord> records = financeRecordRepository.findByRecordDateBetweenOrderByRecordDateAsc(
                startCal.getTime(), endCal.getTime());
        
        // 初始化月度数据
        for (int i = 0; i < 12; i++) {
            incomeList.add(BigDecimal.ZERO);
            expenseList.add(BigDecimal.ZERO);
            profitList.add(BigDecimal.ZERO);
        }
        
        // 按月汇总数据
        for (FinanceRecord record : records) {
            Calendar recordCal = Calendar.getInstance();
            recordCal.setTime(record.getRecordDate());
            int month = recordCal.get(Calendar.MONTH);
            
            // 累加当月数据
            BigDecimal currentIncome = incomeList.get(month);
            BigDecimal currentExpense = expenseList.get(month);
            BigDecimal currentProfit = profitList.get(month);
            
            incomeList.set(month, currentIncome.add(record.getIncome()));
            expenseList.set(month, currentExpense.add(record.getExpense()));
            profitList.set(month, currentProfit.add(record.getProfit()));
        }
        
        // Get monthly order data from OrderService
        Map<String, List<?>> orderData = orderService.getMonthlyTypedOrderData(year);
        
        @SuppressWarnings("unchecked")
        List<Integer> salesOrderQuantityList = (List<Integer>) orderData.get("salesOrderCounts");
        @SuppressWarnings("unchecked")
        List<Integer> purchaseOrderQuantityList = (List<Integer>) orderData.get("purchaseOrderCounts");
        @SuppressWarnings("unchecked")
        List<BigDecimal> salesTotalAmountList = (List<BigDecimal>) orderData.get("salesTotalAmounts");
        @SuppressWarnings("unchecked")
        List<BigDecimal> purchaseTotalAmountList = (List<BigDecimal>) orderData.get("purchaseTotalAmounts");
        
        result.put("income", incomeList);
        result.put("expense", expenseList);
        result.put("profit", profitList);
        result.put("salesOrderQuantity", salesOrderQuantityList);
        result.put("purchaseOrderQuantity", purchaseOrderQuantityList);
        result.put("salesTotalAmounts", salesTotalAmountList);
        result.put("purchaseTotalAmounts", purchaseTotalAmountList);
        
        return result;
    }
} 