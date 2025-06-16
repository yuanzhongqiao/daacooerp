package com.daacooerp.erp.service;

import com.daacooerp.erp.entity.User;
import com.daacooerp.erp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户服务测试类
 * 测试用户相关的业务逻辑
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordService passwordService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("rawPassword");
        testUser.setTel("13800138000");
        testUser.setEmail("test@example.com");
        testUser.setRole("USER");
        testUser.setStatus(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 测试根据用户名查找用户
     */
    @Test
    void testFindByUsername() {
        // 模拟仓库行为
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // 执行测试
        Optional<User> result = userService.findByUsername("testuser");

        // 验证结果
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("13800138000", result.get().getTel());

        // 验证仓库方法调用
        verify(userRepository).findByUsername("testuser");
    }

    /**
     * 测试根据用户名查找用户 - 用户不存在
     */
    @Test
    void testFindByUsernameNotFound() {
        // 模拟仓库行为
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // 执行测试
        Optional<User> result = userService.findByUsername("nonexistent");

        // 验证结果
        assertFalse(result.isPresent());

        // 验证仓库方法调用
        verify(userRepository).findByUsername("nonexistent");
    }

    /**
     * 测试根据电话号码查找用户
     */
    @Test
    void testFindByTel() {
        // 模拟仓库行为
        when(userRepository.findByTel("13800138000")).thenReturn(Optional.of(testUser));

        // 执行测试
        Optional<User> result = userService.findByTel("13800138000");

        // 验证结果
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("13800138000", result.get().getTel());

        // 验证仓库方法调用
        verify(userRepository).findByTel("13800138000");
    }

    /**
     * 测试创建新用户
     */
    @Test
    void testCreateUser() {
        // 准备测试数据
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("rawPassword");
        newUser.setTel("13900139000");
        newUser.setEmail("newuser@example.com");

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setUsername("newuser");
        savedUser.setPassword("encodedPassword");
        savedUser.setTel("13900139000");
        savedUser.setEmail("newuser@example.com");
        savedUser.setRole("user");
        savedUser.setStatus(true);

        // 模拟服务行为
        when(passwordService.encodePassword("rawPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // 执行测试
        User result = userService.createUser(newUser);

        // 验证结果
        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("newuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals("user", result.getRole());
        assertTrue(result.getStatus());

        // 验证服务方法调用
        verify(passwordService).encodePassword("rawPassword");
        verify(userRepository).save(any(User.class));
    }

    /**
     * 测试创建新用户 - 已有角色
     */
    @Test
    void testCreateUserWithExistingRole() {
        // 准备测试数据
        User newUser = new User();
        newUser.setUsername("adminuser");
        newUser.setPassword("rawPassword");
        newUser.setRole("ADMIN");

        User savedUser = new User();
        savedUser.setId(3L);
        savedUser.setUsername("adminuser");
        savedUser.setPassword("encodedPassword");
        savedUser.setRole("ADMIN");
        savedUser.setStatus(true);

        // 模拟服务行为
        when(passwordService.encodePassword("rawPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // 执行测试
        User result = userService.createUser(newUser);

        // 验证结果
        assertNotNull(result);
        assertEquals("ADMIN", result.getRole());
        assertTrue(result.getStatus());

        // 验证服务方法调用
        verify(passwordService).encodePassword("rawPassword");
        verify(userRepository).save(any(User.class));
    }

    /**
     * 测试更新用户信息
     */
    @Test
    void testUpdateUser() {
        // 准备测试数据
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setTel("13800138001");
        updatedUser.setEmail("updated@example.com");

        // 模拟仓库行为
        when(userRepository.save(updatedUser)).thenReturn(updatedUser);

        // 执行测试
        User result = userService.updateUser(updatedUser);

        // 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("13800138001", result.getTel());
        assertEquals("updated@example.com", result.getEmail());

        // 验证仓库方法调用
        verify(userRepository).save(updatedUser);
    }

    /**
     * 测试验证密码
     */
    @Test
    void testVerifyPassword() {
        // 模拟密码服务行为
        when(passwordService.matches("rawPassword", "encodedPassword")).thenReturn(true);
        when(passwordService.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // 执行测试 - 正确密码
        boolean result1 = userService.verifyPassword("rawPassword", "encodedPassword");
        assertTrue(result1);

        // 执行测试 - 错误密码
        boolean result2 = userService.verifyPassword("wrongPassword", "encodedPassword");
        assertFalse(result2);

        // 验证密码服务方法调用
        verify(passwordService).matches("rawPassword", "encodedPassword");
        verify(passwordService).matches("wrongPassword", "encodedPassword");
    }

    /**
     * 测试检查用户名是否存在
     */
    @Test
    void testIsUsernameExists() {
        // 模拟仓库行为
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        // 执行测试 - 用户名存在
        boolean result1 = userService.isUsernameExists("existinguser");
        assertTrue(result1);

        // 执行测试 - 用户名不存在
        boolean result2 = userService.isUsernameExists("newuser");
        assertFalse(result2);

        // 验证仓库方法调用
        verify(userRepository).existsByUsername("existinguser");
        verify(userRepository).existsByUsername("newuser");
    }

    /**
     * 测试检查电话号码是否存在
     */
    @Test
    void testIsTelExists() {
        // 模拟仓库行为
        when(userRepository.existsByTel("13800138000")).thenReturn(true);
        when(userRepository.existsByTel("13900139000")).thenReturn(false);

        // 执行测试 - 电话号码存在
        boolean result1 = userService.isTelExists("13800138000");
        assertTrue(result1);

        // 执行测试 - 电话号码不存在
        boolean result2 = userService.isTelExists("13900139000");
        assertFalse(result2);

        // 验证仓库方法调用
        verify(userRepository).existsByTel("13800138000");
        verify(userRepository).existsByTel("13900139000");
    }

    /**
     * 测试更新登录时间
     */
    @Test
    void testUpdateLoginTime() {
        // 模拟仓库行为
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setLastLogin(LocalDateTime.now());
            return user;
        });

        // 执行测试
        userService.updateLoginTime(testUser);

        // 验证仓库方法调用
        verify(userRepository).save(testUser);
    }
}