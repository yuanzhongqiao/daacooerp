package com.daacooerp.erp.controller;

import com.daacooerp.erp.common.Result;
import com.daacooerp.erp.entity.Inventory;
import com.daacooerp.erp.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin
public class InventoryController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InventoryController.class);

    @Autowired
    private InventoryService inventoryService;

    /**
     * 获取库存列表
     */
    @GetMapping("/list")
    public Result getInventoryList(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            log.info("接收到获取库存列表请求: page={}, size={}", page, size);

            // 检查inventoryService是否正确注入
            if (inventoryService == null) {
                log.error("inventoryService未正确注入");
                return Result.error("系统错误：服务组件未初始化");
            }

            try {
                // 调用服务层获取库存列表
                Page<Inventory> inventoryPage = inventoryService.getInventoryList(page, size);

                // 检查返回的库存列表是否为null
                if (inventoryPage == null) {
                    log.warn("inventoryService.getInventoryList()返回了null");
                    return Result.success(new java.util.ArrayList<>());
                }

                log.info("成功返回库存列表，数量: {}", inventoryPage.getSize());
                return Result.success(inventoryPage);
            } catch (org.springframework.dao.DataAccessException e) {
                log.error("数据库访问错误: {}", e.getMessage(), e);
                return Result.error("数据库访问错误: " + e.getMessage());
            } catch (RuntimeException e) {
                log.error("获取库存列表失败(RuntimeException): {}", e.getMessage(), e);
                return Result.error("获取库存列表失败: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("获取库存列表失败(未预期的异常): {}", e.getMessage(), e);
            return Result.error("系统错误，请联系管理员");
        }
    }

    /**
     * 获取所有商品名称列表（用于自动提示）
     */
    @GetMapping("/product-names")
    public Result<java.util.List<String>> getAllProductNames() {
        try {
            log.info("获取所有商品名称列表");
            java.util.List<String> productNames = inventoryService.getAllProductNames();
            log.info("获取商品名称列表成功，数量: {}", productNames.size());
            return Result.success(productNames);
        } catch (Exception e) {
            log.error("获取商品名称列表失败: {}", e.getMessage(), e);
            return Result.error("获取商品名称列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据商品名称获取库存详情（用于自动填充价格）
     */
    @GetMapping("/by-name/{productName}")
    public Result<Inventory> getInventoryByProductName(@PathVariable String productName) {
        try {
            log.info("根据商品名称获取库存详情: {}", productName);
            Inventory inventory = inventoryService.findByProductName(productName);
            if (inventory != null) {
                log.info("找到库存记录: 商品={}, 价格={}, 数量={}",
                    inventory.getProductName(), inventory.getUnitPrice(), inventory.getQuantity());
                return Result.success(inventory);
            } else {
                log.info("未找到商品库存记录: {}", productName);
                return Result.error("未找到该商品的库存记录");
            }
        } catch (Exception e) {
            log.error("根据商品名称获取库存详情失败: {}", e.getMessage(), e);
            return Result.error("获取商品库存详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取单个库存详情
     */
    @GetMapping("/{id}")
    public Result getInventoryById(@PathVariable Long id) {
        try {
            log.info("接收到获取库存详情请求: id={}", id);
            Inventory inventory = inventoryService.getInventoryById(id);
            return Result.success(inventory);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            log.warn("请求的库存不存在: {}", e.getMessage());
            return Result.error("库存不存在: " + e.getMessage());
        } catch (Exception e) {
            log.error("获取库存详情失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建库存
     */
    @PostMapping
    public Result createInventory(@RequestBody Inventory inventory) {
        try {
            log.info("接收到创建库存请求: {}", inventory.getProductName());
            Inventory savedInventory = inventoryService.createInventory(inventory);
            log.info("库存创建成功: id={}", savedInventory.getId());
            return Result.success(savedInventory);
        } catch (Exception e) {
            log.error("创建库存失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新库存
     */
    @PutMapping("/{id}")
    public Result updateInventory(@PathVariable Long id, @RequestBody Inventory inventory) {
        try {
            log.info("接收到更新库存请求: id={}", id);
            inventory.setId(id);
            Inventory updatedInventory = inventoryService.updateInventory(inventory);
            log.info("库存更新成功: id={}", updatedInventory.getId());
            return Result.success(updatedInventory);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            log.warn("要更新的库存不存在: {}", e.getMessage());
            return Result.error("库存不存在: " + e.getMessage());
        } catch (Exception e) {
            log.error("更新库存失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除库存
     */
    @DeleteMapping("/{id}")
    public Result deleteInventory(@PathVariable Long id) {
        try {
            log.info("接收到删除库存请求: id={}", id);
            inventoryService.deleteInventory(id);
            log.info("库存删除成功: id={}", id);
            return Result.success();
        } catch (jakarta.persistence.EntityNotFoundException e) {
            log.warn("要删除的库存不存在: {}", e.getMessage());
            return Result.error("库存不存在: " + e.getMessage());
        } catch (Exception e) {
            log.error("删除库存失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 库存入库
     */
    @PostMapping("/stock-in")
    public Result stockIn(@RequestBody Inventory inventory) {
        try {
            log.info("接收到库存入库请求: id={}, quantity={}", inventory.getId(), inventory.getQuantity());
            Inventory updatedInventory = inventoryService.stockIn(inventory);
            log.info("库存入库成功: id={}, 新数量={}", updatedInventory.getId(), updatedInventory.getQuantity());
            return Result.success(updatedInventory);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            log.warn("要入库的库存不存在: {}", e.getMessage());
            return Result.error("库存不存在: " + e.getMessage());
        } catch (Exception e) {
            log.error("库存入库失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 库存出库
     */
    @PostMapping("/stock-out")
    public Result stockOut(@RequestBody Inventory inventory) {
        try {
            log.info("接收到库存出库请求: id={}, quantity={}", inventory.getId(), inventory.getQuantity());
            Inventory updatedInventory = inventoryService.stockOut(inventory);
            log.info("库存出库成功: id={}, 新数量={}", updatedInventory.getId(), updatedInventory.getQuantity());
            return Result.success(updatedInventory);
        } catch (IllegalArgumentException e) {
            log.warn("库存不足: {}", e.getMessage());
            return Result.error("库存不足: " + e.getMessage());
        } catch (jakarta.persistence.EntityNotFoundException e) {
            log.warn("要出库的库存不存在: {}", e.getMessage());
            return Result.error("库存不存在: " + e.getMessage());
        } catch (Exception e) {
            log.error("库存出库失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

}