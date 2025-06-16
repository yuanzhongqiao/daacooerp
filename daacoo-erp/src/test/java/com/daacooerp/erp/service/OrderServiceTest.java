package com.daacooerp.erp.service;

import com.daacooerp.erp.entity.Goods;
import com.daacooerp.erp.entity.Order;
import com.daacooerp.erp.entity.OrderGoods;
import com.daacooerp.erp.entity.User;
import com.daacooerp.erp.entity.Inventory;
import com.daacooerp.erp.entity.FinanceRecord;
import com.daacooerp.erp.repository.GoodsRepository;
import com.daacooerp.erp.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private GoodsRepository goodsRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private FinanceService financeService;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private Goods testGoods;
    private OrderGoods testOrderGoods;
    private List<Order> orderList;
    private User testUser;

    @BeforeEach
    public void setup() {
        // 创建测试商品
        testGoods = new Goods();
        testGoods.setId(1L);
        testGoods.setName("测试商品");
        testGoods.setSellingPrice(99.99f);
        testGoods.setStock(100);
        testGoods.setCategory("电子产品");
        testGoods.setDescription("测试用商品");
        testGoods.setCreatedAt(LocalDateTime.now());
        testGoods.setUpdatedAt(LocalDateTime.now());

        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setTel("13800138000");
        testUser.setRole("ADMIN");
        testUser.setStatus(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        // 创建测试订单商品
        testOrderGoods = new OrderGoods();
        testOrderGoods.setId(1L);
        testOrderGoods.setGoods(testGoods);
        testOrderGoods.setQuantity(2);
        testOrderGoods.setUnitPrice(99.99f);
        testOrderGoods.setTotalPrice(199.98f);
        testOrderGoods.setCreatedAt(LocalDateTime.now());
        testOrderGoods.setUpdatedAt(LocalDateTime.now());
        
        // 创建测试订单
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNo("SO202312150001");
        testOrder.setOrderType("SALE");
        testOrder.setCustomerName("测试客户");
        testOrder.setAmount(199.98f);
        testOrder.setStatus("PENDING");
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());
        
        testOrderGoods.setOrder(testOrder);
        testOrder.setGoods(Arrays.asList(testOrderGoods));
        
        // 创建测试订单列表
        orderList = Arrays.asList(testOrder);
    }

    @Test
    public void testGetOrderList() {
        // 准备测试数据
        List<Order> orders = new ArrayList<>();
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD123456");
        order.setOrderType("SALE");
        orders.add(order);

        Page<Order> orderPage = new PageImpl<>(orders);

        // 模拟仓库层行为
        when(orderRepository.findAll(any(Pageable.class))).thenReturn(orderPage);

        // 执行测试
        Page<Order> result = orderService.getOrderList(0, 10);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("ORD123456", result.getContent().get(0).getOrderNo());
        assertEquals("SALE", result.getContent().get(0).getOrderType());

        // 验证调用
        verify(orderRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    public void testCreateOrder_SaleOrder() {
        // 准备测试数据
        Order order = new Order();
        order.setType("customer"); // 前端传递的类型
        order.setCustomerName("测试客户");

        List<OrderGoods> goods = new ArrayList<>();
        OrderGoods orderGoods = new OrderGoods();
        
        Goods goodsItem = new Goods();
        goodsItem.setName("测试商品");
        goodsItem.setStock(10);
        orderGoods.setGoods(goodsItem);
        orderGoods.setQuantity(2);
        orderGoods.setUnitPrice(100.0f);
        goods.add(orderGoods);
        
        order.setGoods(goods);

        // 模拟仓库层行为
        when(goodsRepository.findByName(anyString())).thenReturn(new ArrayList<>());
        when(goodsRepository.save(any(Goods.class))).thenReturn(goodsItem);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            savedOrder.setId(1L);
            return savedOrder;
        });

        // 执行测试
        Order result = orderService.createOrder(order, goods);

        // 验证结果
        assertNotNull(result);
        assertEquals("SALE", result.getOrderType()); // 验证类型转换
        assertNotNull(result.getOrderNo()); // 验证生成订单编号
        assertEquals(1L, result.getId()); // 验证ID设置

        // 验证调用
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    public void testGetOrdersByType() {
        // 准备测试数据
        List<Order> orders = new ArrayList<>();
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD123456");
        order.setOrderType("SALE");
        orders.add(order);

        Page<Order> orderPage = new PageImpl<>(orders);

        // 模拟仓库层行为
        when(orderRepository.findByOrderType(anyString(), any(Pageable.class))).thenReturn(orderPage);

        // 执行测试
        Page<Order> result = orderService.getOrdersByType("customer", 0, 10);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("SALE", result.getContent().get(0).getOrderType());

        // 验证调用
        verify(orderRepository, times(1)).findByOrderType(eq("SALE"), any(Pageable.class));
    }

    @Test
    public void testConfirmOrder_SaleOrder() {
        // 准备测试数据
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD123456");
        order.setOrderType("SALE");
        order.setStatus("PENDING");
        
        List<OrderGoods> goods = new ArrayList<>();
        OrderGoods orderGoods = new OrderGoods();
        
        Goods goodsItem = new Goods();
        goodsItem.setId(1L);
        goodsItem.setName("测试商品");
        goodsItem.setStock(10);
        orderGoods.setGoods(goodsItem);
        orderGoods.setQuantity(2);
        orderGoods.setUnitPrice(100.0f);
        goods.add(orderGoods);
        
        order.setGoods(goods);

        Inventory inventory = new Inventory();
        inventory.setId(1L);
        inventory.setProductName("测试商品");
        inventory.setQuantity(10);
        
        Inventory stockOutData = new Inventory();
        stockOutData.setId(1L);
        stockOutData.setQuantity(2);
        
        // 创建出库后的库存对象
        Inventory updatedInventory = new Inventory();
        updatedInventory.setId(1L);
        updatedInventory.setProductName("测试商品");
        updatedInventory.setQuantity(8); // 10 - 2 = 8
        
        // 创建财务记录对象
        FinanceRecord financeRecord = new FinanceRecord();
        financeRecord.setId(1L);

        // 模拟仓库层行为
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));
        when(inventoryService.findByProductName(anyString())).thenReturn(inventory);
        when(inventoryService.stockOut(any(Inventory.class))).thenReturn(updatedInventory);
        when(goodsRepository.save(any(Goods.class))).thenReturn(goodsItem);
        when(financeService.createFinanceRecord(any(FinanceRecord.class))).thenReturn(financeRecord);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // 执行测试
        Order result = orderService.confirmOrder(1L, 20.0f);

        // 验证结果
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus()); // 验证状态更新
        assertEquals(20.0f, result.getFreight()); // 验证运费设置

        // 验证调用
        verify(orderRepository, times(1)).findById(anyLong());
        verify(inventoryService, times(2)).findByProductName(anyString());
        verify(inventoryService, times(1)).stockOut(any(Inventory.class));
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    public void testGetOrderById() {
        // 准备测试数据
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD123456");
        order.setOrderType("SALE");

        // 模拟仓库层行为
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));

        // 执行测试
        Order result = orderService.getOrderById(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ORD123456", result.getOrderNo());
        assertEquals("SALE", result.getOrderType());

        // 验证调用
        verify(orderRepository, times(1)).findById(anyLong());
    }
}