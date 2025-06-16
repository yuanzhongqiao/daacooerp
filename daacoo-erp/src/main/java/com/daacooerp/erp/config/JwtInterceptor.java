package com.daacooerp.erp.config;

import com.daacooerp.erp.entity.User;
import com.daacooerp.erp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;


@Component
public class JwtInterceptor implements HandlerInterceptor {
    
    @Autowired
    private JwtConfig jwtConfig;
    
    @Autowired
    private UserService userService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        System.out.println("🔒 JWT拦截器处理请求: " + method + " " + requestURI);
        
        try {
            String token = request.getHeader("Authorization");
            System.out.println("🎫 请求头中的Authorization: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));
            
            if (token != null && token.startsWith("Bearer ")) {
                // 提取实际的token（去掉"Bearer "前缀）
                String actualToken = token.substring(7);
                System.out.println("🔑 提取的实际token长度: " + actualToken.length());
                
                // 验证token
                boolean isValid = jwtConfig.validateToken(actualToken);
                System.out.println("✅ Token验证结果: " + (isValid ? "有效" : "无效"));
                
                if (isValid) {
                    // 从token中获取用户名
                    String username = jwtConfig.getUsernameFromToken(actualToken);
                    System.out.println("👤 从token中解析的用户名: " + username);
                    
                    if (username != null && !username.trim().isEmpty()) {
                        // 验证用户是否存在于数据库中
                        Optional<User> userOpt = userService.findByUsername(username);
                        System.out.println("🔍 数据库中查找用户结果: " + (userOpt.isPresent() ? "找到" : "未找到"));
                        
                        if (userOpt.isPresent()) {
                            User user = userOpt.get();
                            System.out.println("👤 用户状态: " + (user.getStatus() ? "活跃" : "禁用"));
                            
                            if (user.getStatus()) {
                                // 设置用户信息到请求属性中
                                request.setAttribute("username", username);
                                request.setAttribute("userId", user.getId());
                                System.out.println("✅ JWT认证成功，允许访问");
                                return true;
                            } else {
                                System.err.println("❌ 用户账户已被禁用: " + username);
                                return sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "账户已被禁用");
                            }
                        } else {
                            System.err.println("❌ 数据库中未找到用户: " + username);
                            return sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "用户不存在");
                        }
                    } else {
                        System.err.println("❌ 无法从token中解析用户名");
                        return sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "无效的token格式");
                    }
                } else {
                    System.err.println("❌ Token验证失败");
                    return sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token已过期或无效");
                }
            } else {
                System.err.println("❌ 缺少Authorization头或格式不正确");
                return sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "缺少认证信息");
            }
            
        } catch (Exception e) {
            System.err.println("❌ JWT拦截器处理异常: " + e.getMessage());
            e.printStackTrace();
            return sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "认证处理异常: " + e.getMessage());
        }
    }
    
    /**
     * 发送错误响应
     */
    private boolean sendErrorResponse(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        String errorJson = String.format("{\"code\":%d,\"error\":\"%s\"}", status, message);
        response.getWriter().write(errorJson);
        return false;
    }
}