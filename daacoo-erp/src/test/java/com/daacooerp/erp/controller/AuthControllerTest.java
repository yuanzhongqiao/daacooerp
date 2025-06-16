package com.daacooerp.erp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.daacooerp.erp.config.JwtConfig;
import com.daacooerp.erp.config.TestConfig;
import com.daacooerp.erp.entity.User;
import com.daacooerp.erp.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户认证控制器测试类
 * 测试用户登录、注册、获取用户信息等功能
 */
@SpringBootTest
@AutoConfigureWebMvc
@Import(TestConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-for-jwt-testing-purposes-123456789",
    "jwt.expiration=86400000"
})
@ActiveProfiles("test")
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtConfig jwtConfig;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 设置MockMvc，使用测试配置（不包含JWT拦截器）
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();
        
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
    }

    /**
     * 测试用户登录成功
     */
    @Test
    void testLoginSuccess() throws Exception {
        // 准备测试数据
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "testuser");
        loginRequest.put("password", "password123");

        // 模拟服务层行为
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userService.verifyPassword("password123", "encodedPassword")).thenReturn(true);
        when(jwtConfig.generateToken("testuser")).thenReturn("mock-jwt-token");
        doNothing().when(userService).updateLoginTime(any(User.class));

        // 执行请求并验证结果
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.token").value("Bearer mock-jwt-token"));

        // 验证服务层方法调用
        verify(userService).findByUsername("testuser");
        verify(userService).verifyPassword("password123", "encodedPassword");
        verify(userService).updateLoginTime(testUser);
        verify(jwtConfig).generateToken("testuser");
    }

    /**
     * 测试用户注册成功
     */
    @Test
    void testRegisterSuccess() throws Exception {
        // 准备测试数据
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("username", "newuser");
        registerRequest.put("password", "password123");
        registerRequest.put("tel", "13900139000");
        registerRequest.put("email", "newuser@example.com");

        // 模拟服务层行为
        when(userService.isUsernameExists("newuser")).thenReturn(false);
        when(userService.isTelExists("13900139000")).thenReturn(false);
        when(userService.createUser(any(User.class))).thenReturn(testUser);

        // 执行请求并验证结果
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("注册成功"));

        // 验证服务层方法调用
        verify(userService).isUsernameExists("newuser");
        verify(userService).isTelExists("13900139000");
        verify(userService).createUser(any(User.class));
    }

    /**
     * 测试获取用户信息成功
     */
    @Test
    void testGetUserInfoSuccess() throws Exception {
        // 模拟服务层行为
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        // 模拟JWT验证
        when(jwtConfig.validateToken("valid-token")).thenReturn(true);
        when(jwtConfig.getUsernameFromToken("valid-token")).thenReturn("testuser");

        // 执行请求并验证结果 - 添加JWT token头
        mockMvc.perform(get("/api/auth/user")
                .header("Authorization", "Bearer valid-token")
                .requestAttr("username", "testuser")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("testuser"))
                .andExpect(jsonPath("$.data.roles[0]").value("USER"))
                .andExpect(jsonPath("$.data.tel").value("13800138000"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));

        // 验证服务层方法调用
        verify(userService, times(2)).findByUsername("testuser");
    }

    /**
     * 测试用户登录失败 - 用户名或密码错误
     */
    @Test
    void testLoginFailure() throws Exception {
        // 准备测试数据
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "testuser");
        loginRequest.put("password", "wrongpassword");

        // 模拟服务层行为
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userService.verifyPassword("wrongpassword", "encodedPassword")).thenReturn(false);

        // 执行请求并验证结果
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        // 验证服务层方法调用
        verify(userService).findByUsername("testuser");
        verify(userService).verifyPassword("wrongpassword", "encodedPassword");
        verify(userService, never()).updateLoginTime(any(User.class));
        verify(jwtConfig, never()).generateToken(anyString());
    }

    /**
     * 测试用户登录失败 - 参数为空
     */
    @Test
    void testLoginWithEmptyParameters() throws Exception {
        // 准备测试数据
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "");
        loginRequest.put("password", null);

        // 执行请求并验证结果
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名和密码不能为空"));

        // 验证服务层方法未被调用
        verify(userService, never()).findByUsername(anyString());
        verify(userService, never()).verifyPassword(anyString(), anyString());
    }

    /**
     * 测试用户注册失败 - 用户名已存在
     */
    @Test
    void testRegisterFailureUsernameExists() throws Exception {
        // 准备测试数据
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("username", "existinguser");
        registerRequest.put("password", "password123");
        registerRequest.put("tel", "13900139000");

        // 模拟服务层行为
        when(userService.isUsernameExists("existinguser")).thenReturn(true);

        // 执行请求并验证结果
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名已存在"));

        // 验证服务层方法调用
        verify(userService).isUsernameExists("existinguser");
        verify(userService, never()).createUser(any(User.class));
    }

    /**
     * 测试用户注册失败 - 必要字段为空
     */
    @Test
    void testRegisterFailureEmptyFields() throws Exception {
        // 准备测试数据
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("username", "newuser");
        registerRequest.put("password", null);
        registerRequest.put("tel", "");

        // 执行请求并验证结果
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名、密码和电话号码不能为空"));

        // 验证服务层方法未被调用
        verify(userService, never()).isUsernameExists(anyString());
        verify(userService, never()).createUser(any(User.class));
    }
}