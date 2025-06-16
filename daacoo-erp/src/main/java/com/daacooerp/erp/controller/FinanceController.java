package com.daacooerp.erp.controller;

import com.daacooerp.erp.common.Result;
import com.daacooerp.erp.entity.Finance;
import com.daacooerp.erp.entity.FinanceRecord;
import com.daacooerp.erp.service.FinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    @Autowired
    private FinanceService financeService;

    @GetMapping("/{year}")
    public Result<Finance> getFinanceByYear(@PathVariable Integer year) {
        try {
            Map<String, Object> monthlyData = financeService.getMonthlyFinanceData(year);
            
            // 转换为Finance对象
            List<BigDecimal> profitList = (List<BigDecimal>) monthlyData.get("profit");
            List<BigDecimal> turnoverList = (List<BigDecimal>) monthlyData.get("income");
            
            // 转换为Float和Integer类型，与Finance实体类匹配
            List<Float> profit = profitList.stream()
                    .map(BigDecimal::floatValue)
                    .collect(Collectors.toList());
            
            List<Float> turnover = turnoverList.stream()
                    .map(BigDecimal::floatValue)
                    .collect(Collectors.toList());
            
            // 生成订单数量（这里可以从订单表中获取实际数据）
            // List<Integer> orderQuantity = new ArrayList<>();
            // for (int i = 0; i < 12; i++) {
            //     orderQuantity.add(0); // 这里应该从订单表中获取实际数据
            // }
            @SuppressWarnings("unchecked")
            List<Integer> salesOrderQuantity = (List<Integer>) monthlyData.get("salesOrderQuantity");
            @SuppressWarnings("unchecked")
            List<Integer> purchaseOrderQuantity = (List<Integer>) monthlyData.get("purchaseOrderQuantity");
            @SuppressWarnings("unchecked")
            List<BigDecimal> salesTotalAmountsBigDecimal = (List<BigDecimal>) monthlyData.get("salesTotalAmounts");
            @SuppressWarnings("unchecked")
            List<BigDecimal> purchaseTotalAmountsBigDecimal = (List<BigDecimal>) monthlyData.get("purchaseTotalAmounts");

            // Convert BigDecimal lists to Float lists
            List<Float> salesTotalAmounts = salesTotalAmountsBigDecimal.stream()
                    .map(bd -> bd != null ? bd.floatValue() : 0.0f)
                    .collect(Collectors.toList());
            List<Float> purchaseTotalAmounts = purchaseTotalAmountsBigDecimal.stream()
                    .map(bd -> bd != null ? bd.floatValue() : 0.0f)
                    .collect(Collectors.toList());
            
            Finance finance = new Finance(profit, turnover, salesOrderQuantity, purchaseOrderQuantity, salesTotalAmounts, purchaseTotalAmounts);
            return Result.success(finance);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取年度财务数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getFinanceStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Date start = parseDate(startDate);
            Date end = parseDate(endDate);
            
            if (start == null) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.YEAR, -1); // 默认查询最近一年
                start = cal.getTime();
            }
            
            if (end == null) {
                end = new Date();
            }
            
            Map<String, Object> statistics = financeService.getFinanceStatistics(start, end);
            
            // 转换为前端需要的格式
            Map<String, Object> result = new HashMap<>();
            result.put("totalProfit", statistics.get("totalProfit"));
            result.put("totalTurnover", statistics.get("totalIncome"));
            result.put("totalOrderQuantity", statistics.get("recordCount"));
            
            // 计算平均值
            BigDecimal totalIncome = (BigDecimal) statistics.get("totalIncome");
            BigDecimal totalProfit = (BigDecimal) statistics.get("totalProfit");
            Long recordCount = (Long) statistics.get("recordCount");
            
            if (recordCount > 0) {
                result.put("averageProfit", totalProfit.divide(BigDecimal.valueOf(recordCount), 2, BigDecimal.ROUND_HALF_UP));
                result.put("averageTurnover", totalIncome.divide(BigDecimal.valueOf(recordCount), 2, BigDecimal.ROUND_HALF_UP));
            } else {
                result.put("averageProfit", BigDecimal.ZERO);
                result.put("averageTurnover", BigDecimal.ZERO);
            }
            
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取财务统计数据失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/data")
    public Result<List<Map<String, Object>>> getFinanceData(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Date start = parseDate(startDate);
            Date end = parseDate(endDate);
            
            if (start == null) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 1);
                start = cal.getTime();
            }
            
            if (end == null) {
                end = new Date();
            }
            
            // 获取财务记录
            List<FinanceRecord> records = financeService.getFinanceRecords(start, end);
            
            // 转换为前端需要的格式
            List<Map<String, Object>> result = new ArrayList<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            
            for (FinanceRecord record : records) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", record.getId());
                item.put("date", dateFormat.format(record.getRecordDate()));
                item.put("income", record.getIncome());
                item.put("expense", record.getExpense());
                item.put("profit", record.getProfit());
                item.put("type", record.getRecordType());
                item.put("description", record.getDescription());
                result.add(item);
            }
            
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取财务数据失败: " + e.getMessage());
        }
    }
    
    // 创建财务记录
    @PostMapping
    public Result<FinanceRecord> createFinanceRecord(@RequestBody FinanceRecord record) {
        try {
            FinanceRecord created = financeService.createFinanceRecord(record);
            return Result.success(created);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "创建财务记录失败: " + e.getMessage());
        }
    }
    
    // 更新财务记录
    @PutMapping("/{id}")
    public Result<FinanceRecord> updateFinanceRecord(
            @PathVariable Long id, 
            @RequestBody FinanceRecord record) {
        try {
            FinanceRecord updated = financeService.updateFinanceRecord(id, record);
            return Result.success(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "更新财务记录失败: " + e.getMessage());
        }
    }
    
    // 删除财务记录
    @DeleteMapping("/{id}")
    public Result<Void> deleteFinanceRecord(@PathVariable Long id) {
        try {
            financeService.deleteFinanceRecord(id);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "删除财务记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析日期字符串
     */
    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        
        try {
            // 尝试解析ISO格式日期
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return isoFormat.parse(dateStr);
        } catch (ParseException e) {
            try {
                // 尝试解析标准日期格式
                SimpleDateFormat stdFormat = new SimpleDateFormat("yyyy-MM-dd");
                return stdFormat.parse(dateStr);
            } catch (ParseException ex) {
                throw new IllegalArgumentException("无效的日期格式: " + dateStr);
            }
        }
    }
}