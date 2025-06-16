package com.daacooerp.erp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public WebMvcConfigurer testWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                // 在测试环境中不添加任何拦截器
                // 这样JWT拦截器就不会被注册
            }
        };
    }
} 