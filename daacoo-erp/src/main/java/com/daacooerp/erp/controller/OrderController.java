package com.daacooerp.erp.controller;

import com.daacooerp.erp.common.Result;
import com.daacooerp.erp.entity.Order;
import com.daacooerp.erp.entity.OrderGoods;
import com.daacooerp.erp.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@RestController
@RequestMapping("/api/customer-order")
@CrossOrigin(origins = {"http://localhost:5174", "http://localhost:9876"}, allowCredentials = "true")
public class OrderController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderController.class);
    
    @Autowired
    private OrderService orderService;
    
    /**
     * 获取订单列表，支持分页
     */
    @GetMapping("/list")
    public Result<Page<Order>> getOrderList(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            log.info("接收到获取订单列表请求: page={}, size={}", page, size);
            Page<Order> orderPage = orderService.getOrderList(page, size);
            log.info("成功返回订单列表，总数: {}, 当前页数量: {}", 
                orderPage.getTotalElements(), orderPage.getContent().size());
            return Result.success(orderPage);
        } catch (Exception e) {
            log.error("获取订单列表失败: {}", e.getMessage(), e);
            return Result.error("获取订单列表失败: " + e.getMessage());
        }
    }
    
    @PostMapping
    public Result<Order> createOrder(@RequestBody Order order) {
        try {
            log.info("接收到订单创建请求，前端type: {}, orderType: {}", order.getType(), order.getOrderType());
            
            List<OrderGoods> goods = order.getGoods();
            if (goods == null || goods.isEmpty()) {
                return Result.error("订单商品不能为空");
            }
            
            // 添加调试日志，检查商品价格信息
            for (int i = 0; i < goods.size(); i++) {
                OrderGoods item = goods.get(i);
                log.info("商品 {} - 名称: {}, 数量: {}, 单价: {}, 总价: {}", 
                    i, 
                    item.getGoods() != null ? item.getGoods().getName() : "null",
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getTotalPrice());
            }
            
            // 强制设置正确的订单类型 - 关键修复
            if (order.getType() != null) {
                if ("customer".equalsIgnoreCase(order.getType())) {
                    order.setOrderType("SALE");
                    log.info("设置销售订单类型: SALE");
                } else if ("purchase".equalsIgnoreCase(order.getType())) {
                    order.setOrderType("PURCHASE");
                    log.info("设置采购订单类型: PURCHASE");
                } else {
                    log.warn("未知的前端订单类型: {}, 默认设置为销售订单", order.getType());
                    order.setOrderType("SALE");
                }
            } else if (order.getOrderType() != null) {
                if ("SALE".equals(order.getOrderType())) {
                    order.setType("customer");
                } else if ("PURCHASE".equals(order.getOrderType())) {
                    order.setType("purchase");
                } else {
                    log.warn("未知的订单类型: {}, 默认设置为销售订单", order.getOrderType());
                    order.setOrderType("SALE");
                    order.setType("customer");
                }
            } else {
                // 都为空时，默认为销售订单
                order.setOrderType("SALE");
                order.setType("customer");
                log.info("订单类型为空，默认设置为销售订单");
            }
            
            log.info("最终订单类型 - type: {}, orderType: {}", order.getType(), order.getOrderType());
            
            // 调用服务层创建订单
            Order createdOrder = orderService.createOrder(order, goods);
            log.info("订单创建成功: ID={}, orderType={}", createdOrder.getId(), createdOrder.getOrderType());
            
            return Result.success(createdOrder);
        } catch (Exception e) {
            log.error("创建订单失败: {}", e.getMessage(), e);
            return Result.error("创建订单失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return Result.success();
    }
    
    @PostMapping("/{id}/confirm")
    public Result<Order> confirmOrder(
            @PathVariable Long id, 
            @RequestParam float freight) {
        return Result.success(orderService.confirmOrder(id, freight));
    }
    
    @GetMapping("/type/{type}")
    public Result<Page<Order>> getOrdersByType(
            @PathVariable String type,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            log.info("接收到获取特定类型订单的请求: type={}, page={}, size={}", type, page, size);
            Page<Order> orderPage = orderService.getOrdersByType(type, page, size);
            log.info("成功返回{}类型订单列表，总数: {}, 当前页数量: {}", 
                type, orderPage.getTotalElements(), orderPage.getContent().size());
            return Result.success(orderPage);
        } catch (Exception e) {
            log.error("获取{}类型订单列表失败: {}", type, e.getMessage(), e);
            return Result.error("获取订单列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public Result<Order> getOrderDetail(@PathVariable Long id) {
        try {
            log.info("获取订单详情: id={}", id);
            Order order = orderService.getOrderById(id);
            if (order == null) {
                return Result.error("订单不存在");
            }
            log.info("成功获取订单详情: ID={}, 商品数量={}", order.getId(), 
                order.getGoods() != null ? order.getGoods().size() : 0);
            return Result.success(order);
        } catch (Exception e) {
            log.error("获取订单详情失败: {}", e.getMessage(), e);
            return Result.error("获取订单详情失败: " + e.getMessage());
        }
    }
}