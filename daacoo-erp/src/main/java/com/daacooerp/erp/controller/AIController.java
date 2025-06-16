package com.daacooerp.erp.controller;

import com.daacooerp.erp.service.AIService;
import com.daacooerp.erp.service.external.DeepSeekAIService;
import com.daacooerp.erp.dto.AIRequest;
import com.daacooerp.erp.dto.AIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.concurrent.*;

/**
 * AI控制器
 * 处理自然语言理解和AI分析相关的HTTP请求
 */
@RestController
@RequestMapping("/ai")
public class AIController {

    @Autowired
    private AIService aiService;
    
    @Autowired
    private DeepSeekAIService deepSeekAIService;
    
    // 用于处理超时的线程池
    private ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 解析自然语言并执行相应操作
     */
    @PostMapping("/parse")
    public AIResponse parse(@RequestBody AIRequest request) {
        return aiService.parseAndExecute(request.getInput(), request.isConfirmed());
    }

    /**
     * 获取业务洞察分析 - 优化版本
     * 使用线程池和超时机制，确保即使AI处理超时也能返回基础分析
     */
    @PostMapping("/insights")
    public AIResponse getInsights(@RequestBody AIRequest request) {
        try {
            // 创建带超时的Future任务
            Future<AIResponse> future = executorService.submit(() -> {
                try {
                    return aiService.getBusinessInsights(request);
                } catch (Exception e) {
                    return generateBackupResponse(e, request);
                }
            });
            
            // 等待任务完成，设置超时时间
            return future.get(90, TimeUnit.SECONDS);
            
        } catch (TimeoutException e) {
            // 超时情况下返回基本分析结果
            System.err.println("AI分析超时：" + e.getMessage());
            return generateBackupResponse(e, request);
        } catch (Exception e) {
            // 其他异常情况
            System.err.println("AI分析异常：" + e.getMessage());
            return generateBackupResponse(e, request);
        }
    }
    
    /**
     * 生成备用响应，避免用户等待过长时间
     */
    private AIResponse generateBackupResponse(Exception e, AIRequest request) {
        try {
            String analysisType = request.getAnalysisType() != null ? request.getAnalysisType() : "GENERAL";
            
            // 尝试生成基本的本地分析结果
            String backupAnalysis = deepSeekAIService.generateLocalAnalysis(
                request.getInput(),
                request.getDataContext(),
                analysisType
            );
            
            if (backupAnalysis != null && !backupAnalysis.trim().isEmpty()) {
                return new AIResponse("📊 " + backupAnalysis, false);
            } else {
                return new AIResponse("😅 AI分析服务暂时不可用，请查看基础数据或稍后重试。", false);
            }
        } catch (Exception ex) {
            return new AIResponse("😅 分析服务暂时不可用: " + e.getMessage(), false);
        }
    }

    /**
     * AI服务状态检查
     */
    @GetMapping("/status")
    public Map<String, Object> getAIStatus() {
        return deepSeekAIService.getServiceStatus();
    }

    /**
     * AI服务健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        boolean healthy = deepSeekAIService.healthCheck();
        return Map.of(
            "healthy", healthy,
            "status", healthy ? "OK" : "ERROR",
            "timestamp", System.currentTimeMillis()
        );
    }
} 