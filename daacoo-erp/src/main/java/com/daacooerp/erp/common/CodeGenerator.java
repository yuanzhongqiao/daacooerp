package com.daacooerp.erp.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 编码生成器工具类
 */
public class CodeGenerator {
    
    private static final Random random = new Random();
    
    /**
     * 生成商品编码
     * 格式：P + 年月日 + 4位随机数字
     * 例：P20241125001234
     */
    public static String generateProductCode() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%04d", random.nextInt(10000));
        return "P" + dateStr + randomStr;
    }
    
    /**
     * 根据商品分类生成编码
     * 格式：分类前缀 + 年月日 + 4位随机数字
     */
    public static String generateProductCodeByCategory(String category) {
        String prefix = getCategoryPrefix(category);
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%04d", random.nextInt(10000));
        return prefix + dateStr + randomStr;
    }
    
    /**
     * 根据分类获取前缀
     */
    private static String getCategoryPrefix(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "P"; // 默认前缀
        }
        
        String lowerCategory = category.toLowerCase();
        
        // 电子产品
        if (lowerCategory.contains("电脑") || lowerCategory.contains("计算机") || 
            lowerCategory.contains("服务器") || lowerCategory.contains("电子")) {
            return "E"; // Electronics
        }
        
        // 办公用品
        if (lowerCategory.contains("办公") || lowerCategory.contains("文具") || 
            lowerCategory.contains("用品")) {
            return "O"; // Office
        }
        
        // 家具
        if (lowerCategory.contains("家具") || lowerCategory.contains("桌") || 
            lowerCategory.contains("椅") || lowerCategory.contains("柜")) {
            return "F"; // Furniture
        }
        
        // 设备
        if (lowerCategory.contains("设备") || lowerCategory.contains("机器") || 
            lowerCategory.contains("仪器")) {
            return "D"; // Device
        }
        
        // 材料
        if (lowerCategory.contains("材料") || lowerCategory.contains("原料") || 
            lowerCategory.contains("配件")) {
            return "M"; // Material
        }
        
        // 默认产品前缀
        return "P";
    }
    
    /**
     * 生成订单编码
     * 格式：SO/PO + 年月日 + 6位随机数字
     */
    public static String generateOrderCode(String orderType) {
        String prefix = "sales".equalsIgnoreCase(orderType) ? "SO" : "PO";
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%06d", random.nextInt(1000000));
        return prefix + dateStr + randomStr;
    }
} 