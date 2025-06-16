package com.daacooerp.erp.service;

import com.daacooerp.erp.entity.Order;
import com.daacooerp.erp.entity.OrderGoods;
import com.daacooerp.erp.entity.User;
import com.daacooerp.erp.entity.Goods;
import com.daacooerp.erp.entity.Inventory;
import com.daacooerp.erp.entity.FinanceRecord;
import com.daacooerp.erp.repository.OrderRepository;
import com.daacooerp.erp.repository.GoodsRepository;
import com.daacooerp.erp.service.InventoryService;
import com.daacooerp.erp.service.FinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.math.BigDecimal;

@Service
public class OrderService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private FinanceService financeService;

    /**
     * 获取订单列表，支持分页
     */
    @Transactional(readOnly = true)
    public Page<Order> getOrderList(Integer page, Integer size) {
        log.info("获取订单列表: page={}, size={}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            return orderRepository.findAll(pageable);
        } catch (Exception e) {
            log.error("获取订单列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取订单列表失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Order createOrder(Order order, List<OrderGoods> goods) {
        log.info("开始创建订单，前端type: {}, orderType: {}", order.getType(), order.getOrderType());

        try {
            // 生成订单编号
            if (order.getOrderNo() == null || order.getOrderNo().isEmpty()) {
                String orderNoPrefix = "ORD";
                order.setOrderNo(orderNoPrefix + System.currentTimeMillis());
                log.info("生成订单编号: {}", order.getOrderNo());
            }

            // 确保订单类型正确设置 - 这是关键修复
            String orderType = order.getOrderType();
            if (orderType == null || orderType.isEmpty()) {
                if (order.getType() != null) {
                    orderType = order.getType().equalsIgnoreCase("customer") ? "SALE" : "PURCHASE";
                    order.setOrderType(orderType);
                    log.info("从type字段设置订单类型: {}", orderType);
                } else {
                    order.setOrderType("SALE"); // 默认为销售订单
                    log.info("设置默认订单类型: SALE");
                }
            } else {
                // 验证和修正orderType值
                if (!"SALE".equals(orderType) && !"PURCHASE".equals(orderType)) {
                    if (order.getType() != null) {
                        orderType = order.getType().equalsIgnoreCase("customer") ? "SALE" : "PURCHASE";
                        order.setOrderType(orderType);
                        log.warn("修正无效的订单类型，新类型: {}", orderType);
                    } else {
                        order.setOrderType("SALE");
                        log.warn("修正无效的订单类型为默认值: SALE");
                    }
                }
            }
            
            log.info("最终订单类型: {}", order.getOrderType());

            // 计算订单总金额
            float totalAmount = 0.0f;
            
            // 设置商品关联
            if (goods != null && !goods.isEmpty()) {
                log.info("处理订单商品，数量: {}", goods.size());
                for (OrderGoods item : goods) {
                    if (item.getGoods() == null) {
                        throw new RuntimeException("订单商品中的商品对象不能为空");
                    }

                    // 处理商品关联 - 如果只有名称没有ID，尝试根据名称查找或创建商品
                    Goods goodsItem = item.getGoods();
                    if (goodsItem.getId() == null && goodsItem.getName() != null) {
                        List<Goods> existingGoods = goodsRepository.findByName(goodsItem.getName());

                        if (!existingGoods.isEmpty()) {
                            goodsItem = existingGoods.get(0);
                            log.info("使用现有商品: {}", goodsItem.getName());
                            
                            // 如果是销售订单，检查库存是否足够
                            if ("SALE".equals(orderType) && goodsItem.getStock() < item.getQuantity()) {
                                log.warn("商品库存不足: {}, 当前库存: {}, 需要: {}", 
                                    goodsItem.getName(), goodsItem.getStock(), item.getQuantity());
                                // 销售订单创建时只警告，不阻止创建，等确认时再严格检查
                            }
                        } else {
                            // 创建新商品
                            goodsItem.setCode("G" + System.currentTimeMillis());
                            goodsItem.setStock(0); // 新商品初始库存为0
                            goodsItem.setStatus(1);
                            // 设置商品价格为订单中的单价
                            if (item.getUnitPrice() != null) {
                                goodsItem.setSellingPrice(item.getUnitPrice());
                                goodsItem.setPurchasePrice(item.getUnitPrice());
                            }
                            goodsItem = goodsRepository.save(goodsItem);
                            log.info("创建新商品: {}", goodsItem.getName());
                        }
                        item.setGoods(goodsItem);
                    }

                    // 确保订单商品的价格信息正确
                    if (item.getUnitPrice() != null && item.getQuantity() != null) {
                        item.setTotalPrice(item.getUnitPrice() * item.getQuantity());
                        totalAmount += item.getTotalPrice();
                        log.info("商品价格计算 - 名称: {}, 单价: {}, 数量: {}, 总价: {}", 
                            goodsItem.getName(), item.getUnitPrice(), item.getQuantity(), item.getTotalPrice());
                    } else {
                        log.warn("商品价格信息不完整 - 名称: {}, 单价: {}, 数量: {}", 
                            goodsItem.getName(), item.getUnitPrice(), item.getQuantity());
                    }
                    
                    item.setOrder(order);
                }
                order.setGoods(goods);
            } else {
                order.setGoods(new java.util.ArrayList<>());
            }

            // 设置订单总金额
            if (order.getAmount() == null || order.getAmount() == 0.0f) {
                order.setAmount(totalAmount);
                log.info("设置订单总金额: {}", totalAmount);
            }

            // 保存订单
            Order savedOrder = orderRepository.save(order);
            log.info("订单保存成功: ID={}, 类型={}, 金额={}, 状态={}", 
                    savedOrder.getId(), savedOrder.getOrderType(), savedOrder.getAmount(), savedOrder.getStatus());
            
            // 🆕 移除自动确认功能，保持订单在PENDING状态
            // 用户需要手动确认订单才会更新库存和财务记录
            // 这样用户可以完全控制确认过程
            
            return savedOrder;
        } catch (Exception e) {
            log.error("创建订单失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建订单失败: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<Order> getOrdersByType(String type, Integer page, Integer size) {
        try {
            log.info("获取类型为{}的订单: page={}, size={}", type, page, size);

            try {
                Pageable pageable = PageRequest.of(page, size);

                // 根据类型获取订单
                if ("customer".equalsIgnoreCase(type) || "SALE".equalsIgnoreCase(type)) {
                    // 获取销售订单
                    return orderRepository.findByOrderType("SALE", pageable);
                } else if ("purchase".equalsIgnoreCase(type) || "PURCHASE".equalsIgnoreCase(type)) {
                    // 获取采购订单
                    return orderRepository.findByOrderType("PURCHASE", pageable);
                } else {
                    log.warn("未知的订单类型: {}", type);
                    return Page.empty();
                }
            } catch (Exception e) {
                log.error("获取类型为{}的订单列表失败: {}", type, e.getMessage(), e);
                throw new RuntimeException("获取订单列表失败: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("获取类型为{}的订单列表时发生错误: {}", type, e.getMessage(), e);
            throw new RuntimeException("获取订单列表失败: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    @Transactional
    public Order confirmOrder(Long id, float freight) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("订单不存在"));

        if ("COMPLETED".equals(order.getStatus())) {
            throw new RuntimeException("订单请勿重复确认");
        }
        
        // 如果是销售订单，先检查所有商品库存是否足够
        if ("SALE".equals(order.getOrderType())) {
            for (OrderGoods orderGoods : order.getGoods()) {
                Goods goods = orderGoods.getGoods();
                Integer quantity = orderGoods.getQuantity();
                
                // 检查库存
                Inventory inventory = inventoryService.findByProductName(goods.getName());
                if (inventory == null || inventory.getQuantity() < quantity) {
                    String errorMsg = "库存不足，无法确认订单。商品: " + goods.getName();
                    if (inventory != null) {
                        errorMsg += ", 当前库存: " + inventory.getQuantity() + ", 需要: " + quantity;
                    } else {
                        errorMsg += ", 库存中未找到该商品";
                    }
                    log.error(errorMsg);
                    throw new RuntimeException(errorMsg);
                }
            }
        }

        // 订单确认后自动更新库存
        updateInventoryOnOrderConfirm(order);

        // 订单确认后自动创建财务记录
        createFinanceRecordOnOrderConfirm(order);

        order.setStatus("COMPLETED");
        order.setFreight(freight);
        return orderRepository.save(order);
    }

    /**
     * 订单确认时自动更新库存
     */
    private void updateInventoryOnOrderConfirm(Order order) {
        try {
            log.info("开始更新订单库存，订单ID: {}, 订单类型: {}", order.getId(), order.getOrderType());

            for (OrderGoods orderGoods : order.getGoods()) {
                Goods goods = orderGoods.getGoods();
                Integer quantity = orderGoods.getQuantity();
                Double unitPrice = orderGoods.getUnitPrice() != null ? orderGoods.getUnitPrice().doubleValue() : null;

                if ("PURCHASE".equals(order.getOrderType())) {
                    // 采购订单确认：增加库存
                    log.info("采购订单确认，增加库存: 商品={}, 数量={}", goods.getName(), quantity);
                    inventoryService.createOrUpdateInventoryFromGoods(
                        goods.getName(),
                        goods.getCode(),
                        quantity,
                        unitPrice
                    );
                    
                    // 同步更新Goods表中的库存数量
                    goods.setStock(goods.getStock() + quantity);
                    goodsRepository.save(goods);
                    log.info("更新商品库存: 商品={}, 新库存={}", goods.getName(), goods.getStock());
                } else if ("SALE".equals(order.getOrderType())) {
                    // 销售订单确认：减少库存
                    log.info("销售订单确认，减少库存: 商品={}, 数量={}", goods.getName(), quantity);
                    Inventory inventory = inventoryService.findByProductName(goods.getName());
                    if (inventory != null) {
                        if (inventory.getQuantity() < quantity) {
                            throw new RuntimeException("库存不足，商品: " + goods.getName() +
                                ", 当前库存: " + inventory.getQuantity() +
                                ", 需要: " + quantity);
                        }
                        // 减少库存
                        Inventory stockOutData = new Inventory();
                        stockOutData.setId(inventory.getId());
                        stockOutData.setQuantity(quantity);
                        inventoryService.stockOut(stockOutData);
                        
                        // 同步更新Goods表中的库存数量
                        if (goods.getStock() < quantity) {
                            goods.setStock(0);
                            log.warn("商品表库存数据不一致，已重置为0: 商品={}", goods.getName());
                        } else {
                            goods.setStock(goods.getStock() - quantity);
                        }
                        goodsRepository.save(goods);
                        log.info("更新商品库存: 商品={}, 新库存={}", goods.getName(), goods.getStock());
                    } else {
                        throw new RuntimeException("库存中未找到商品: " + goods.getName());
                    }
                }
            }

            log.info("订单库存更新完成，订单ID: {}", order.getId());
        } catch (Exception e) {
            log.error("订单库存更新失败，订单ID: {}, 错误: {}", order.getId(), e.getMessage(), e);
            throw new RuntimeException("库存更新失败: " + e.getMessage(), e);
        }
    }

    /**
     * 订单确认时自动创建财务记录
     */
    private void createFinanceRecordOnOrderConfirm(Order order) {
        try {
            log.info("开始创建订单财务记录，订单ID: {}, 订单类型: {}", order.getId(), order.getOrderType());

            FinanceRecord financeRecord = new FinanceRecord();
            financeRecord.setRecordDate(new java.util.Date());
            financeRecord.setCreatedBy("system"); // 系统自动创建

            // 计算订单总金额
            java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
            for (OrderGoods orderGoods : order.getGoods()) {
                log.info("财务记录计算 - 商品: {}, 单价: {}, 数量: {}, 总价: {}", 
                    orderGoods.getGoods() != null ? orderGoods.getGoods().getName() : "null",
                    orderGoods.getUnitPrice(),
                    orderGoods.getQuantity(),
                    orderGoods.getTotalPrice());
                if (orderGoods.getTotalPrice() != null) {
                    totalAmount = totalAmount.add(java.math.BigDecimal.valueOf(orderGoods.getTotalPrice()));
                }
            }
            
            log.info("财务记录总金额计算结果: {}", totalAmount);

            if ("PURCHASE".equals(order.getOrderType())) {
                // 采购订单：记录为支出
                financeRecord.setExpense(totalAmount);
                financeRecord.setIncome(java.math.BigDecimal.ZERO);
                financeRecord.setRecordType("PURCHASE");
                financeRecord.setDescription("采购订单自动记录 - 订单号: " + order.getOrderNo());
                log.info("采购订单确认，记录支出: 金额={}", totalAmount);
            } else if ("SALE".equals(order.getOrderType())) {
                // 销售订单：记录为收入
                financeRecord.setIncome(totalAmount);
                financeRecord.setExpense(java.math.BigDecimal.ZERO);
                financeRecord.setRecordType("SALES");
                financeRecord.setDescription("销售订单自动记录 - 订单号: " + order.getOrderNo());
                log.info("销售订单确认，记录收入: 金额={}", totalAmount);
            }

            // 保存财务记录
            financeService.createFinanceRecord(financeRecord);
            log.info("订单财务记录创建完成，订单ID: {}", order.getId());

        } catch (Exception e) {
            log.error("订单财务记录创建失败，订单ID: {}, 错误: {}", order.getId(), e.getMessage(), e);
            // 财务记录创建失败不影响订单确认，只记录日志
        }
    }

    @Transactional(readOnly = true)
    public Map<String, List<?>> getMonthlyTypedOrderData(int year) {
        List<Map<String, Object>> monthlyStats = orderRepository.getMonthlyOrderStatisticsByType(year);
        
        List<Integer> salesOrderCounts = new ArrayList<>(12);
        List<BigDecimal> salesTotalAmounts = new ArrayList<>(12);
        List<Integer> purchaseOrderCounts = new ArrayList<>(12);
        List<BigDecimal> purchaseTotalAmounts = new ArrayList<>(12);

        for (int i = 0; i < 12; i++) {
            salesOrderCounts.add(0);
            salesTotalAmounts.add(BigDecimal.ZERO);
            purchaseOrderCounts.add(0);
            purchaseTotalAmounts.add(BigDecimal.ZERO);
        }

        for (Map<String, Object> stat : monthlyStats) {
            Integer month = (Integer) stat.get("month");
            String orderType = (String) stat.get("orderType");
            Long count = (Long) stat.get("orderCount");
            Double amount = (Double) stat.get("totalAmount"); 

            if (month != null && month >= 1 && month <= 12) {
                if ("SALE".equals(orderType)) {
                    salesOrderCounts.set(month - 1, count != null ? count.intValue() : 0);
                    salesTotalAmounts.set(month - 1, amount != null ? BigDecimal.valueOf(amount) : BigDecimal.ZERO);
                } else if ("PURCHASE".equals(orderType)) {
                    purchaseOrderCounts.set(month - 1, count != null ? count.intValue() : 0);
                    purchaseTotalAmounts.set(month - 1, amount != null ? BigDecimal.valueOf(amount) : BigDecimal.ZERO);
                }
            }
        }

        Map<String, List<?>> result = new HashMap<>();
        result.put("salesOrderCounts", salesOrderCounts);
        result.put("salesTotalAmounts", salesTotalAmounts);
        result.put("purchaseOrderCounts", purchaseOrderCounts);
        result.put("purchaseTotalAmounts", purchaseTotalAmounts);
        return result;
    }

    /**
     * 根据ID获取订单详情
     */
    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        log.info("获取订单详情: id={}", id);
        try {
            return orderRepository.findById(id).orElse(null);
        } catch (Exception e) {
            log.error("获取订单详情失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取订单详情失败: " + e.getMessage(), e);
        }
    }
}