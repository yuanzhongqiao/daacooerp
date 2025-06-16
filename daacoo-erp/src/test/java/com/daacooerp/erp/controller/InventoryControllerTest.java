package com.daacooerp.erp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.daacooerp.erp.config.JwtConfig;
import com.daacooerp.erp.config.TestConfig;
import com.daacooerp.erp.entity.Inventory;
import com.daacooerp.erp.entity.User;
import com.daacooerp.erp.service.InventoryService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 库存管理控制器测试类
 * 测试库存的增删改查等功能
 */
@SpringBootTest
@AutoConfigureWebMvc
@Import(TestConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-for-jwt-testing-purposes-123456789",
    "jwt.expiration=86400000"
})
@ActiveProfiles("test")
public class InventoryControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtConfig jwtConfig;

    @Autowired
    private ObjectMapper objectMapper;

    private Inventory testInventory;
    private List<Inventory> inventoryList;
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
                
        testInventory = new Inventory();
        testInventory.setId(1L);
        testInventory.setProductName("测试商品");
        testInventory.setProductCode("TEST001");
        testInventory.setQuantity(100);
        testInventory.setUnit("个");
        testInventory.setUnitPrice(10.50);
        testInventory.setLocation("A区-01");
        testInventory.setCategory("电子产品");
        testInventory.setWarningThreshold(10);
        testInventory.setCreatedAt(LocalDateTime.now());
        testInventory.setUpdatedAt(LocalDateTime.now());

        Inventory inventory2 = new Inventory();
        inventory2.setId(2L);
        inventory2.setProductName("测试商品2");
        inventory2.setProductCode("TEST002");
        inventory2.setQuantity(50);
        inventory2.setUnit("件");
        inventory2.setUnitPrice(25.00);
        inventory2.setLocation("B区-02");
        inventory2.setCategory("办公用品");
        inventory2.setWarningThreshold(5);
        inventory2.setCreatedAt(LocalDateTime.now());
        inventory2.setUpdatedAt(LocalDateTime.now());

        inventoryList = Arrays.asList(testInventory, inventory2);
    }

    /**
     * 测试获取库存列表
     */
    @Test
    void testGetInventoryList() throws Exception {
        // 模拟服务层行为
        Page<Inventory> inventoryPage = new PageImpl<>(inventoryList);
        when(inventoryService.getInventoryList(0, 10)).thenReturn(inventoryPage);

        // 执行请求并验证结果
        mockMvc.perform(get("/api/inventory/list")
                .header("Authorization", "Bearer " + VALID_TOKEN)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].productName").value("测试商品"))
                .andExpect(jsonPath("$.data.content[0].productCode").value("TEST001"))
                .andExpect(jsonPath("$.data.content[0].quantity").value(100))
                .andExpect(jsonPath("$.data.content[1].productName").value("测试商品2"));

        // 验证服务层方法调用
        verify(inventoryService).getInventoryList(0, 10);
    }

    /**
     * 测试获取库存列表 - 使用默认分页参数
     */
    @Test
    void testGetInventoryListWithDefaultParams() throws Exception {
        // 模拟服务层行为
        Page<Inventory> inventoryPage = new PageImpl<>(inventoryList);
        when(inventoryService.getInventoryList(0, 10)).thenReturn(inventoryPage);

        // 执行请求并验证结果
        mockMvc.perform(get("/api/inventory/list")
                .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray());

        // 验证服务层方法调用
        verify(inventoryService).getInventoryList(0, 10);
    }

    /**
     * 测试获取所有商品名称列表
     */
    @Test
    void testGetAllProductNames() throws Exception {
        // 模拟服务层行为
        List<String> productNames = Arrays.asList("测试商品", "测试商品2", "测试商品3");
        when(inventoryService.getAllProductNames()).thenReturn(productNames);

        // 执行请求并验证结果
        mockMvc.perform(get("/api/inventory/product-names")
                .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0]").value("测试商品"))
                .andExpect(jsonPath("$.data[1]").value("测试商品2"))
                .andExpect(jsonPath("$.data[2]").value("测试商品3"));

        // 验证服务层方法调用
        verify(inventoryService).getAllProductNames();
    }

    /**
     * 测试根据商品名称获取库存详情
     */
    @Test
    void testGetInventoryByProductName() throws Exception {
        // 模拟服务层行为
        when(inventoryService.findByProductName("测试商品")).thenReturn(testInventory);

        // 执行请求并验证结果
        mockMvc.perform(get("/api/inventory/by-name/测试商品")
                .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.productName").value("测试商品"))
                .andExpect(jsonPath("$.data.productCode").value("TEST001"))
                .andExpect(jsonPath("$.data.quantity").value(100))
                .andExpect(jsonPath("$.data.unitPrice").value(10.5));

        // 验证服务层方法调用
        verify(inventoryService).findByProductName("测试商品");
    }

    /**
     * 测试根据商品名称获取库存详情 - 商品不存在
     */
    @Test
    void testGetInventoryByProductNameNotFound() throws Exception {
        // 模拟服务层行为
        when(inventoryService.findByProductName("不存在的商品")).thenReturn(null);

        // 执行请求并验证结果
        mockMvc.perform(get("/api/inventory/by-name/不存在的商品")
                .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("未找到该商品的库存记录"));

        // 验证服务层方法调用
        verify(inventoryService).findByProductName("不存在的商品");
    }

    /**
     * 测试获取单个库存详情
     */
    @Test
    void testGetInventoryById() throws Exception {
        // 模拟服务层行为
        when(inventoryService.getInventoryById(1L)).thenReturn(testInventory);

        // 执行请求并验证结果
        mockMvc.perform(get("/api/inventory/1")
                .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.productName").value("测试商品"))
                .andExpect(jsonPath("$.data.productCode").value("TEST001"));

        // 验证服务层方法调用
        verify(inventoryService).getInventoryById(1L);
    }

    /**
     * 测试创建库存
     */
    @Test
    void testCreateInventory() throws Exception {
        // 准备测试数据
        Inventory newInventory = new Inventory();
        newInventory.setProductName("新商品");
        newInventory.setQuantity(200);
        newInventory.setUnit("箱");
        newInventory.setUnitPrice(15.00);
        newInventory.setLocation("C区-03");
        newInventory.setCategory("食品");
        newInventory.setWarningThreshold(20);

        Inventory savedInventory = new Inventory();
        savedInventory.setId(3L);
        savedInventory.setProductName("新商品");
        savedInventory.setProductCode("AUTO003");
        savedInventory.setQuantity(200);
        savedInventory.setUnit("箱");
        savedInventory.setUnitPrice(15.00);
        savedInventory.setLocation("C区-03");
        savedInventory.setCategory("食品");
        savedInventory.setWarningThreshold(20);
        savedInventory.setCreatedAt(LocalDateTime.now());
        savedInventory.setUpdatedAt(LocalDateTime.now());

        // 模拟服务层行为
        when(inventoryService.createInventory(any(Inventory.class))).thenReturn(savedInventory);

        // 执行请求并验证结果
        mockMvc.perform(post("/api/inventory")
                .header("Authorization", "Bearer " + VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newInventory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.productName").value("新商品"))
                .andExpect(jsonPath("$.data.productCode").value("AUTO003"));

        // 验证服务层方法调用
        verify(inventoryService).createInventory(any(Inventory.class));
    }

    /**
     * 测试更新库存
     */
    @Test
    void testUpdateInventory() throws Exception {
        // 准备测试数据
        Inventory updatedInventory = new Inventory();
        updatedInventory.setId(1L);
        updatedInventory.setProductName("更新后的商品");
        updatedInventory.setProductCode("TEST001");
        updatedInventory.setQuantity(150);
        updatedInventory.setUnit("个");
        updatedInventory.setUnitPrice(12.00);
        updatedInventory.setLocation("A区-01");
        updatedInventory.setCategory("电子产品");
        updatedInventory.setWarningThreshold(15);

        // 模拟服务层行为
        when(inventoryService.updateInventory(any(Inventory.class))).thenReturn(updatedInventory);

        // 执行请求并验证结果
        mockMvc.perform(put("/api/inventory/1")
                .header("Authorization", "Bearer " + VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedInventory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.productName").value("更新后的商品"))
                .andExpect(jsonPath("$.data.quantity").value(150))
                .andExpect(jsonPath("$.data.unitPrice").value(12.0));

        // 验证服务层方法调用
        verify(inventoryService).updateInventory(any(Inventory.class));
    }

    /**
     * 测试删除库存
     */
    @Test
    void testDeleteInventory() throws Exception {
        // 模拟服务层行为
        doNothing().when(inventoryService).deleteInventory(1L);

        // 执行请求并验证结果
        mockMvc.perform(delete("/api/inventory/1")
                .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));

        // 验证服务层方法调用
        verify(inventoryService).deleteInventory(1L);
    }

    /**
     * 测试库存入库
     */
    @Test
    void testStockIn() throws Exception {
        // 准备测试数据
        Inventory stockInData = new Inventory();
        stockInData.setId(1L);
        stockInData.setQuantity(50);

        Inventory updatedInventory = new Inventory();
        updatedInventory.setId(1L);
        updatedInventory.setProductName("测试商品");
        updatedInventory.setQuantity(150); // 100 + 50

        // 模拟服务层行为
        when(inventoryService.stockIn(any(Inventory.class))).thenReturn(updatedInventory);

        // 执行请求并验证结果
        mockMvc.perform(post("/api/inventory/stock-in")
                .header("Authorization", "Bearer " + VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stockInData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.quantity").value(150));

        // 验证服务层方法调用
        verify(inventoryService).stockIn(any(Inventory.class));
    }

    /**
     * 测试库存出库
     */
    @Test
    void testStockOut() throws Exception {
        // 准备测试数据
        Inventory stockOutData = new Inventory();
        stockOutData.setId(1L);
        stockOutData.setQuantity(30);

        Inventory updatedInventory = new Inventory();
        updatedInventory.setId(1L);
        updatedInventory.setProductName("测试商品");
        updatedInventory.setQuantity(70); // 100 - 30

        // 模拟服务层行为
        when(inventoryService.stockOut(any(Inventory.class))).thenReturn(updatedInventory);

        // 执行请求并验证结果
        mockMvc.perform(post("/api/inventory/stock-out")
                .header("Authorization", "Bearer " + VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stockOutData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.quantity").value(70));

        // 验证服务层方法调用
        verify(inventoryService).stockOut(any(Inventory.class));
    }

    /**
     * 测试处理服务层异常
     */
    @Test
    void testHandleServiceException() throws Exception {
        // 模拟服务层抛出异常
        when(inventoryService.getInventoryList(0, 10))
                .thenThrow(new RuntimeException("数据库连接失败"));

        // 执行请求并验证结果
        mockMvc.perform(get("/api/inventory/list")
                .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("获取库存列表失败: 数据库连接失败"));

        // 验证服务层方法调用
        verify(inventoryService).getInventoryList(0, 10);
    }
}