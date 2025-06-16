package com.daacooerp.erp.controller;

import com.daacooerp.erp.common.Result;
import com.daacooerp.erp.entity.Company;
import com.daacooerp.erp.entity.Staff;
import com.daacooerp.erp.entity.User;
import com.daacooerp.erp.service.CompanyService;
import com.daacooerp.erp.service.PasswordService;
import com.daacooerp.erp.service.StaffService;
import com.daacooerp.erp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    @Autowired
    private StaffService staffService;
    
    @Autowired
    private CompanyService companyService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordService passwordService;

    @GetMapping
    public Result<?> getStaffList(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Staff> staffPage = staffService.getAllStaff(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", staffPage.getContent());
        response.put("totalElements", staffPage.getTotalElements());
        response.put("totalPages", staffPage.getTotalPages());
        response.put("number", staffPage.getNumber());
        response.put("size", staffPage.getSize());
        
        return Result.success(response);
    }
    
    @GetMapping("/company/{companyId}")
    public Result<?> getStaffByCompany(
            @PathVariable Long companyId,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        
        // 检查公司是否存在
        Optional<Company> company = companyService.getCompanyById(companyId);
        if (!company.isPresent()) {
            return Result.error("公司不存在");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Staff> staffPage = staffService.getStaffByCompany(companyId, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", staffPage.getContent());
        response.put("totalElements", staffPage.getTotalElements());
        response.put("totalPages", staffPage.getTotalPages());
        response.put("number", staffPage.getNumber());
        response.put("size", staffPage.getSize());
        
        return Result.success(response);
    }
    
    @GetMapping("/{id}")
    public Result<?> getStaffDetail(@PathVariable Long id) {
        Optional<Staff> staff = staffService.getStaffById(id);
        
        if (staff.isPresent()) {
            return Result.success(staff.get());
        } else {
            return Result.error("员工不存在");
        }
    }

    @PostMapping
    public Result<?> createStaff(@RequestBody Staff staff) {
        try {
            Staff savedStaff = staffService.createStaff(staff);
            return Result.success("员工创建成功", savedStaff);
        } catch (Exception e) {
            return Result.error("创建员工失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<?> updateStaff(
            @PathVariable Long id,
            @RequestBody Staff staff) {
        try {
            Staff updatedStaff = staffService.updateStaff(id, staff);
            return Result.success("员工更新成功", updatedStaff);
        } catch (Exception e) {
            return Result.error("更新员工失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteStaff(@PathVariable Long id) {
        try {
            staffService.deleteStaff(id);
            return Result.success("员工删除成功");
        } catch (Exception e) {
            return Result.error("删除员工失败: " + e.getMessage());
        }
    }
    
    @PutMapping("/password")
    public Result<?> updatePassword(@RequestBody Map<String, String> passwordRequest, HttpServletRequest request) {
        try {
            // 从请求属性中获取用户名（由JWT拦截器设置）
            String username = (String) request.getAttribute("username");
            Long userId = (Long) request.getAttribute("userId");
            
            if (username == null || username.trim().isEmpty()) {
                return Result.error(401, "未授权访问：缺少用户信息");
            }
            
            // 获取密码信息
            String oldPassword = passwordRequest.get("oldPassword");
            String newPassword = passwordRequest.get("newPassword");
            
            if (oldPassword == null || newPassword == null) {
                return Result.error(400, "旧密码和新密码不能为空");
            }
            
            // 根据用户名查找用户
            Optional<User> userOpt = userService.findByUsername(username);
            
            if (!userOpt.isPresent()) {
                return Result.error(404, "用户不存在");
            }
            
            User user = userOpt.get();
            
            // 验证旧密码
            if (!userService.verifyPassword(oldPassword, user.getPassword())) {
                return Result.error(400, "旧密码不正确");
            }
            
            // 更新密码
            user.setPassword(passwordService.encodePassword(newPassword));
            userService.updateUser(user);
            
            return Result.success("密码更新成功");
        } catch (Exception e) {
            return Result.error(500, "更新密码失败: " + e.getMessage());
        }
    }
}