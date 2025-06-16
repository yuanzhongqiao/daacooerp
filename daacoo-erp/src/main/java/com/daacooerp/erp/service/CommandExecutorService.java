package com.daacooerp.erp.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 命令执行服务接口
 * 负责解析和执行各种业务命令
 */
public interface CommandExecutorService {
    
    /**
     * 执行JSON格式的命令
     * @param commandJson JSON格式的命令
     * @return 执行结果
     */
    String execute(JsonNode commandJson);
} 