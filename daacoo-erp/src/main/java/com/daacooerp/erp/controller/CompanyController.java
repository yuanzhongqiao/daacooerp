package com.daacooerp.erp.controller;

import com.daacooerp.erp.common.Result;
import com.daacooerp.erp.entity.Company;
import com.daacooerp.erp.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/company")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @GetMapping
    public Result<Map<String, Object>> getCompanyList(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Company> companyPage = companyService.getCompanyList(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", companyPage.getContent());
        response.put("totalElements", companyPage.getTotalElements());
        response.put("totalPages", companyPage.getTotalPages());
        response.put("number", companyPage.getNumber());
        response.put("size", companyPage.getSize());
        
        return Result.success(response);
    }
    
    @GetMapping("/{id}")
    public Result<?> getCompanyDetail(@PathVariable Long id) {
        Optional<Company> company = companyService.getCompanyById(id);
        
        if (company.isPresent()) {
            return Result.success(company.get());
        } else {
            return Result.error("公司不存在");
        }
    }

    @PostMapping
    public Result<?> createCompany(@RequestBody Company company) {
        try {
            Company savedCompany = companyService.createCompany(company);
            return Result.success("公司创建成功", savedCompany);
        } catch (Exception e) {
            return Result.error("创建公司失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<?> updateCompany(
            @PathVariable Long id,
            @RequestBody Company company) {
        try {
            Company updatedCompany = companyService.updateCompany(id, company);
            return Result.success("公司更新成功", updatedCompany);
        } catch (Exception e) {
            return Result.error("更新公司失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteCompany(@PathVariable Long id) {
        try {
            companyService.deleteCompany(id);
            return Result.success("公司删除成功");
        } catch (Exception e) {
            return Result.error("删除公司失败: " + e.getMessage());
        }
    }
}