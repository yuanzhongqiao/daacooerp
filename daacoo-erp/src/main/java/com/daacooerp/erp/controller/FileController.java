package com.daacooerp.erp.controller;

import com.daacooerp.erp.common.Result;
import com.daacooerp.erp.entity.User;
import com.daacooerp.erp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 文件上传控制器
 * 处理系统中的文件上传功能，包括用户头像等
 */
@RestController
@RequestMapping("/api")
public class FileController {
    
    @Autowired
    private UserService userService;
    
    @Value("${file.upload.path:uploads}")
    private String uploadPath;
    
    /**
     * 通用文件上传接口
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // 确保上传目录存在
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString() + extension;
            
            // 保存文件
            Path filePath = Paths.get(uploadPath, newFilename);
            Files.copy(file.getInputStream(), filePath);
            
            // 返回文件URL
            Map<String, String> response = new HashMap<>();
            response.put("url", "/api/uploads/" + newFilename);
            
            return Result.success("文件上传成功", response);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 用户头像上传接口
     */
    @PostMapping("/user/avatar")
    public Result<Map<String, String>> uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            HttpServletRequest request) {
        try {
            // 从请求属性中获取用户名（由JWT拦截器设置）
            String username = (String) request.getAttribute("username");
            Long userId = (Long) request.getAttribute("userId");
            
            if (username == null || username.trim().isEmpty()) {
                return Result.error(401, "未授权访问：缺少用户信息");
            }
            
            // 根据用户名查找用户
            Optional<User> userOpt = userService.findByUsername(username);
            
            if (!userOpt.isPresent()) {
                return Result.error(404, "用户不存在");
            }
            
            User user = userOpt.get();
            
            // 确保上传目录存在
            String avatarPath = uploadPath + "/avatars";
            File uploadDir = new File(avatarPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            
            // 保存文件
            Path filePath = Paths.get(avatarPath, newFilename);
            Files.copy(file.getInputStream(), filePath);
            
            // 更新用户头像URL
            String avatarUrl = "/api/uploads/avatars/" + newFilename;
            
            // 更新用户头像字段
            user.setAvatar(avatarUrl);
            userService.updateUser(user);
            
            // 返回头像URL
            Map<String, String> response = new HashMap<>();
            response.put("avatar", avatarUrl);
            
            return Result.success("头像上传成功", response);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error(500, "头像上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 文件访问接口
     */
    @GetMapping("/uploads/**")
    public void getFile(HttpServletRequest request) {
        // 这里需要实现文件访问逻辑
        // 由于Spring Boot可以自动处理静态资源，我们可以通过配置来实现
        // 在application.properties中添加：
        // spring.mvc.static-path-pattern=/api/uploads/**
        // spring.web.resources.static-locations=file:uploads/
    }
}