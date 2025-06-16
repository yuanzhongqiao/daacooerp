package com.daacooerp.erp.controller;

import com.daacooerp.erp.common.Result;
import com.daacooerp.erp.config.JwtConfig;
import com.daacooerp.erp.entity.User;
import com.daacooerp.erp.service.UserService;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private ServletContext servletContext;

    @Value("${file.upload-dir}")
    private String uploadDir; // = "uploads"
    
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        // 从请求中获取用户名和密码
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        
        // 验证请求参数
        if (username == null || password == null) {
            return Result.error(400, "用户名和密码不能为空");
        }
        
        // 查找用户
        Optional<User> userOpt = userService.findByUsername(username);
        
        // 验证用户名和密码
        if (userOpt.isPresent() && userService.verifyPassword(password, userOpt.get().getPassword())) {
            User user = userOpt.get();
            
            // 更新最后登录时间
            userService.updateLoginTime(user);
            
            // 生成JWT令牌
            String token = jwtConfig.generateToken(user.getUsername());
            Map<String, Object> response = new HashMap<>();
            response.put("token", "Bearer " + token);
            return Result.success("登录成功", response);
        } else {
            return Result.error(401, "用户名或密码错误");
        }
    }
    
    @GetMapping("/user")
    public Result<Map<String, Object>> getUserInfo(HttpServletRequest request) {
        try {
            // 记录调试信息
            System.out.println("🔍 获取用户信息请求开始");
            
            // 从请求属性中获取用户名（由JWT拦截器设置）
            String username = (String) request.getAttribute("username");
            Long userId = (Long) request.getAttribute("userId");
            
            System.out.println("🎯 从请求属性获取到的用户名: " + username);
            System.out.println("🎯 从请求属性获取到的用户ID: " + userId);
            
            if (username == null || username.trim().isEmpty()) {
                System.err.println("❌ 用户名为空，JWT拦截器可能未正确设置用户信息");
                return Result.error(401, "未授权访问：缺少用户信息");
            }
            
            // 根据用户名查找用户
            Optional<User> userOpt = userService.findByUsername(username);
            System.out.println("🔍 数据库查询用户结果: " + (userOpt.isPresent() ? "找到用户" : "用户不存在"));
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // 构建用户信息响应
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("name", user.getUsername());
                userInfo.put("roles", new String[]{user.getRole() != null ? user.getRole() : "USER"});
                userInfo.put("avatar", user.getAvatar() != null ? user.getAvatar() : "https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif");
                userInfo.put("tel", user.getTel() != null ? user.getTel() : "");
                userInfo.put("email", user.getEmail() != null ? user.getEmail() : "");
                
                System.out.println("✅ 成功构建用户信息: " + userInfo);
                return Result.success(userInfo);
            } else {
                System.err.println("❌ 在数据库中未找到用户: " + username);
                return Result.error(404, "用户不存在");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 获取用户信息时发生异常: " + e.getMessage());
            e.printStackTrace();
            return Result.error(500, "服务器内部错误：" + e.getMessage());
        }
    }
    
    @PutMapping("/user")
    public Result<?> updateUserInfo(@RequestBody Map<String, String> userInfoRequest, HttpServletRequest request) {
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
            
            // 更新用户信息
            String email = userInfoRequest.get("email");
            String tel = userInfoRequest.get("tel");
            
            if (email != null) {
                user.setEmail(email);
            }
            
            if (tel != null) {
                user.setTel(tel);
            }
            
            // 保存更新后的用户信息
            userService.updateUser(user);
            
            return Result.success("用户信息更新成功");
        } catch (Exception e) {
            return Result.error(500, "更新用户信息失败: " + e.getMessage());
        }
    }

    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(HttpServletRequest request,
                                                    @RequestParam("avatar") MultipartFile avatar) {
        // 1) 基本校验
        if (avatar == null || avatar.isEmpty()) {
            return Result.error(400, "未选择头像文件");
        }

        // 2) 计算磁盘保存路径
        String realPath = servletContext.getRealPath("/") + File.separator + uploadDir;
        File dir = new File(realPath);
        if (!dir.exists() && !dir.mkdirs()) {
            return Result.error(500, "创建上传目录失败");
        }

        // 3) 生成唯一文件名并保存
        String original = avatar.getOriginalFilename();
        String ext = original != null && original.contains(".")
                ? original.substring(original.lastIndexOf('.'))
                : "";
        String filename = UUID.randomUUID().toString() + ext;
        File dest = new File(dir, filename);
        try {
            avatar.transferTo(dest);
        } catch (IOException e) {
            return Result.error(500, "保存文件失败");
        }

        // 4) 构造可被前端访问的 URL
        String avatarUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/" + uploadDir + "/")
                .path(filename)
                .toUriString();

        // 5) 持久化到数据库
        String username = (String) request.getAttribute("username");
        userService.findByUsername(username).ifPresent(user -> {
            userService.updateAvatar(user, avatarUrl);
        });

        // 6) 给前端返回新 URL
        Map<String, String> data = new HashMap<>();
        data.put("avatarUrl", avatarUrl);
        return Result.success(data);
    }


    @GetMapping("/logout")
    public Result<Void> logout() {
        // 在实际应用中，可能需要处理令牌失效等逻辑
        // 这里简单返回成功响应
        return Result.success("注销成功");
    }
    
    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, String> registerRequest) {
        try {
            String username = registerRequest.get("username");
            String password = registerRequest.get("password");
            String tel = registerRequest.get("tel");
            String email = registerRequest.get("email");
            
            // 验证必要字段不为空
            if (username == null || password == null || tel == null) {
                return Result.error(400, "用户名、密码和电话号码不能为空");
            }
            
            // 检查用户名是否已存在
            if (userService.isUsernameExists(username)) {
                return Result.error(400, "用户名已存在");
            }
            
            // 检查电话号码是否已存在
            if (userService.isTelExists(tel)) {
                return Result.error(400, "电话号码已被注册");
            }
            
            // 创建新用户
            User user = new User();
            user.setUsername(username);
            user.setPassword(password); // UserService会处理密码加密
            user.setTel(tel);
            user.setEmail(email);
            
            // 保存用户
            userService.createUser(user);
            
            return Result.success("注册成功");
            
        } catch (Exception e) {
            System.err.println("❌ 用户注册时发生异常: " + e.getMessage());
            e.printStackTrace();
            return Result.error(500, "注册失败：" + e.getMessage());
        }
    }
}