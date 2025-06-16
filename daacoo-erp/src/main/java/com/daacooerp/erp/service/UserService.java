package com.daacooerp.erp.service;

import com.daacooerp.erp.entity.User;
import com.daacooerp.erp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordService passwordService;
    
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户对象
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * 根据电话号码查找用户
     * @param tel 电话号码
     * @return 用户对象
     */
    public Optional<User> findByTel(String tel) {
        return userRepository.findByTel(tel);
    }
    
    /**
     * 创建新用户
     * @param user 用户对象
     * @return 创建后的用户对象
     */
    public User createUser(User user) {
        // 加密密码
        user.setPassword(passwordService.encodePassword(user.getPassword()));
        // 设置默认角色和状态
        if (user.getRole() == null) {
            user.setRole("user");
        }
        user.setStatus(true);
        return userRepository.save(user);
    }
    
    /**
     * 更新用户信息
     * @param user 用户对象
     * @return 更新后的用户对象
     */
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    /**
     * 更新用户登录时间
     * @param user 用户对象
     */
    public void updateLoginTime(User user) {
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
    }
    
    /**
     * 验证用户密码
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordService.matches(rawPassword, encodedPassword);
    }
    
    /**
     * 检查用户名是否已存在
     * @param username 用户名
     * @return 是否存在
     */
    public boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
    
    /**
     * 检查电话号码是否已存在
     * @param tel 电话号码
     * @return 是否存在
     */
    public boolean isTelExists(String tel) {
        return userRepository.existsByTel(tel);
    }

    public void updateAvatar(User user, String avatarUrl) {
        user.setAvatar(avatarUrl);
        userRepository.save(user);
    }
}