package com.daacooerp.erp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.daacooerp.erp.config.JwtConfig;
import com.daacooerp.erp.config.TestConfig;
import com.daacooerp.erp.entity.Order;
import com.daacooerp.erp.entity.OrderGoods;
import com.daacooerp.erp.entity.User;
import com.daacooerp.erp.service.OrderService;
import com.daacooerp.erp.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureWebMvc
@Import(TestConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-for-jwt-testing-purposes-123456789",
    "jwt.expiration=86400000"
})
@ActiveProfiles("test")
public class OrderControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private OrderService orderService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtConfig jwtConfig;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private final String VALID_TOKEN = "valid-token";

    @BeforeEach
    void setUp() {
        // 设置MockMvc，使用测试配置（不包含JWT拦截器）
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();
        
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setTel("13800138000");
        testUser.setEmail("test@example.com");
        testUser.setRole("USER");
        testUser.setStatus(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        
        // 模拟JWT验证和用户查找
        when(jwtConfig.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtConfig.getUsernameFromToken(VALID_TOKEN)).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    }

    @Test
    public void testGetOrderList() throws Exception {
        // 准备测试数据
        List<Order> orders = new ArrayList<>();
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD123456");
        order.setOrderType("SALE");
        order.setCustomerName("测试客户");
        order.setAmount(1000.0f);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        orders.add(order);

        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 1);

        // 模拟服务层行为
        when(orderService.getOrderList(anyInt(), anyInt())).thenReturn(orderPage);

        // 执行测试
        mockMvc.perform(get("/api/customer-order/list")
                .header("Authorization", "Bearer " + VALID_TOKEN)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].orderNo").value("ORD123456"))
                .andExpect(jsonPath("$.data.content[0].orderType").value("SALE"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    public void testCreateOrder() throws Exception {
        // 准备测试数据
        Order order = new Order();
        order.setOrderNo("ORD123456");
        order.setType("customer"); // 前端传递的类型
        order.setCustomerName("测试客户");
        order.setContactPerson("联系人");
        order.setTel("13800138000");
        order.setAddress("测试地址");
        
        List<OrderGoods> goods = new ArrayList<>();
        OrderGoods orderGoods = new OrderGoods();
        orderGoods.setQuantity(2);
        orderGoods.setUnitPrice(100.0f);
        orderGoods.setTotalPrice(200.0f);
        goods.add(orderGoods);
        order.setGoods(goods);

        Order createdOrder = new Order();
        createdOrder.setId(1L);
        createdOrder.setOrderNo("ORD123456");
        createdOrder.setOrderType("SALE"); // 后端转换后的类型
        createdOrder.setType("customer");
        createdOrder.setCustomerName("测试客户");
        createdOrder.setAmount(200.0f);
        createdOrder.setStatus("PENDING");
        createdOrder.setCreatedAt(LocalDateTime.now());
        createdOrder.setGoods(goods);

        // 模拟服务层行为
        when(orderService.createOrder(any(Order.class), any())).thenReturn(createdOrder);

        // 执行测试
        mockMvc.perform(post("/api/customer-order")
                .header("Authorization", "Bearer " + VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderNo").value("ORD123456"))
                .andExpect(jsonPath("$.data.orderType").value("SALE"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    public void testDeleteOrder() throws Exception {
        // 模拟服务层行为
        doNothing().when(orderService).deleteOrder(anyLong());

        // 执行测试
        mockMvc.perform(delete("/api/customer-order/1")
                .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testConfirmOrder() throws Exception {
        // 准备测试数据
        Order confirmedOrder = new Order();
        confirmedOrder.setId(1L);
        confirmedOrder.setOrderNo("ORD123456");
        confirmedOrder.setOrderType("SALE");
        confirmedOrder.setCustomerName("测试客户");
        confirmedOrder.setAmount(200.0f);
        confirmedOrder.setStatus("COMPLETED"); // 确认后状态变为已完成
        confirmedOrder.setFreight(20.0f); // 设置运费

        // 模拟服务层行为
        when(orderService.confirmOrder(anyLong(), anyFloat())).thenReturn(confirmedOrder);

        // 执行测试
        mockMvc.perform(post("/api/customer-order/1/confirm")
                .header("Authorization", "Bearer " + VALID_TOKEN)
                .param("freight", "20.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.freight").value(20.0));
    }

    @Test
    public void testGetOrdersByType() throws Exception {
        // 准备测试数据
        List<Order> orders = new ArrayList<>();
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD123456");
        order.setOrderType("SALE");
        order.setCustomerName("测试客户");
        order.setAmount(1000.0f);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        orders.add(order);

        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 1);

        // 模拟服务层行为
        when(orderService.getOrdersByType(any(), anyInt(), anyInt())).thenReturn(orderPage);

        // 执行测试
        mockMvc.perform(get("/api/customer-order/type/customer")
                .header("Authorization", "Bearer " + VALID_TOKEN)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].orderType").value("SALE"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    public void testGetOrderDetail() throws Exception {
        // 准备测试数据
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD123456");
        order.setOrderType("SALE");
        order.setCustomerName("测试客户");
        order.setAmount(200.0f);
        order.setStatus("PENDING");
        
        List<OrderGoods> goods = new ArrayList<>();
        OrderGoods orderGoods = new OrderGoods();
        orderGoods.setQuantity(2);
        orderGoods.setUnitPrice(100.0f);
        orderGoods.setTotalPrice(200.0f);
        goods.add(orderGoods);
        order.setGoods(goods);

        // 模拟服务层行为
        when(orderService.getOrderById(anyLong())).thenReturn(order);

        // 执行测试
        mockMvc.perform(get("/api/customer-order/1")
                .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderNo").value("ORD123456"))
                .andExpect(jsonPath("$.data.orderType").value("SALE"))
                .andExpect(jsonPath("$.data.goods").isArray())
                .andExpect(jsonPath("$.data.goods.length()").value(1));
    }
}