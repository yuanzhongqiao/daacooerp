package com.daacooerp.erp.service.impl;

import com.daacooerp.erp.entity.Inventory;
import com.daacooerp.erp.repository.InventoryRepository;
import com.daacooerp.erp.service.InventoryService;
import com.daacooerp.erp.common.CodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Override
    public Page<Inventory> getInventoryList(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return inventoryRepository.findAll(pageable);
    }

    @Override
    public Inventory getInventoryById(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("库存不存在，ID: " + id));
    }

    @Override
    @Transactional
    public Inventory createInventory(Inventory inventory) {
        // 如果商品编码为空，自动生成
        if (inventory.getProductCode() == null || inventory.getProductCode().trim().isEmpty()) {
            String generatedCode;
            if (inventory.getCategory() != null && !inventory.getCategory().trim().isEmpty()) {
                // 根据分类生成编码
                generatedCode = CodeGenerator.generateProductCodeByCategory(inventory.getCategory());
            } else {
                // 使用默认编码格式
                generatedCode = CodeGenerator.generateProductCode();
            }
            inventory.setProductCode(generatedCode);
        }
        
        return inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public Inventory updateInventory(Inventory inventory) {
        // 检查库存是否存在
        if (!inventoryRepository.existsById(inventory.getId())) {
            throw new EntityNotFoundException("库存不存在，ID: " + inventory.getId());
        }
        return inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void deleteInventory(Long id) {
        // 检查库存是否存在
        if (!inventoryRepository.existsById(id)) {
            throw new EntityNotFoundException("库存不存在，ID: " + id);
        }
        inventoryRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Inventory stockIn(Inventory inventoryData) {
        // 获取现有库存
        Inventory existingInventory = getInventoryById(inventoryData.getId());

        // 增加库存数量
        int newQuantity = existingInventory.getQuantity() + inventoryData.getQuantity();
        existingInventory.setQuantity(newQuantity);

        // 更新其他可能变更的字段
        if (inventoryData.getUnitPrice() != null) {
            existingInventory.setUnitPrice(inventoryData.getUnitPrice());
        }
        if (inventoryData.getLocation() != null) {
            existingInventory.setLocation(inventoryData.getLocation());
        }

        return inventoryRepository.save(existingInventory);
    }

    @Override
    @Transactional
    public Inventory stockOut(Inventory inventoryData) {
        // 获取现有库存
        Inventory existingInventory = getInventoryById(inventoryData.getId());

        // 检查库存是否足够
        if (existingInventory.getQuantity() < inventoryData.getQuantity()) {
            throw new IllegalArgumentException("库存不足，当前库存: " + existingInventory.getQuantity());
        }

        // 减少库存数量
        int newQuantity = existingInventory.getQuantity() - inventoryData.getQuantity();
        existingInventory.setQuantity(newQuantity);

        return inventoryRepository.save(existingInventory);
    }

    @Override
    public Inventory findByProductName(String productName) {
        return inventoryRepository.findByProductName(productName).orElse(null);
    }

    @Override
    @Transactional
    public Inventory createOrUpdateInventoryFromGoods(String productName, String productCode, Integer quantity, Double unitPrice) {
        // 先查找是否已存在该商品的库存
        Inventory existingInventory = findByProductName(productName);

        if (existingInventory != null) {
            // 如果已存在，更新数量
            existingInventory.setQuantity(existingInventory.getQuantity() + quantity);
            if (unitPrice != null) {
                existingInventory.setUnitPrice(unitPrice);
            }
            return inventoryRepository.save(existingInventory);
        } else {
            // 如果不存在，创建新的库存记录
            Inventory newInventory = new Inventory();
            newInventory.setProductName(productName);
            
            // 如果没有提供编码或编码为空，自动生成
            if (productCode == null || productCode.trim().isEmpty()) {
                newInventory.setProductCode(CodeGenerator.generateProductCode());
            } else {
                newInventory.setProductCode(productCode);
            }
            
            newInventory.setQuantity(quantity);
            newInventory.setUnitPrice(unitPrice);
            newInventory.setUnit("个"); // 默认单位
            newInventory.setLocation("默认仓库"); // 默认位置
            // newInventory.setWarningThreshold(5); // 默认预警阈值 - 暂时注释掉
            return inventoryRepository.save(newInventory);
        }
    }

    @Override
    public java.util.List<String> getAllProductNames() {
        return inventoryRepository.findAllDistinctProductNames();
    }
}