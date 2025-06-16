package com.daacooerp.erp.service.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import okhttp3.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.net.SocketTimeoutException;

/**
 * 优化的DeepSeek AI服务
 * 支持智能对话、指令解析、业务分析等多种模式
 */
@Service
public class DeepSeekAIService {

    private static final String API_KEY = "sk-ce5b6b6486144537a1ff646c6227a835";
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    //private static final String API_URL = "https://api.deepseek.com/v1";
    // 媒体类型定义
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    // 不同场景的超时配置
    private static final int INTENT_TIMEOUT = 150; // 意图识别：快速响应
    private static final int COMMAND_TIMEOUT = 200; // 指令解析：中等响应
    private static final int CONVERSATION_TIMEOUT = 250; // 对话交流：较长响应
    private static final int ANALYSIS_TIMEOUT = 600; // 业务分析：更长响应（从45秒增加到60秒）
    private static final int ORDER_ANALYSIS_TIMEOUT = 900; // 订单分析：超长响应（从60秒增加到90秒）

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 智能对话模式 - 自然语言交流
     */
    public String chat(String message) throws IOException {
        String systemPrompt = buildChatPrompt();
        return callAIWithRetry(message, systemPrompt, CONVERSATION_TIMEOUT, "CHAT");
    }

    /**
     * 意图识别模式 - 快速判断用户意图
     */
    public String analyzeIntent(String input) throws IOException {
        String systemPrompt = buildIntentPrompt();
        return callAIWithRetry(input, systemPrompt, INTENT_TIMEOUT, "INTENT");
    }

    /**
     * 指令解析模式 - 转换为JSON指令
     */
    public String parseCommand(String input) throws IOException {
        String systemPrompt = buildCommandPrompt();
        return callAIWithRetry(input, systemPrompt, COMMAND_TIMEOUT, "COMMAND");
    }

    /**
     * 业务分析模式 - 深度数据分析
     */
    public String analyzeData(String data, String analysisType) throws IOException {
        // 1. 对输入数据进行预处理和优化
        String processedData = preprocessAnalysisData(data, analysisType);
        
        // 2. 选择合适的系统提示词和超时设置
        String systemPrompt = buildAnalysisPrompt(analysisType);
        int timeout = getAnalysisTimeout(analysisType);
        
        // 3. 使用更高效的分析调用
        return callAnalysisWithOptimizedRetry(processedData, systemPrompt, timeout, analysisType);
    }

    /**
     * 预处理分析数据
     * 优化数据结构和大小，提高AI处理效率
     */
    private String preprocessAnalysisData(String data, String analysisType) {
        if (data == null || data.isEmpty()) {
            return "无数据可分析";
        }
        
        // 如果数据过长，进行智能裁剪
        if (data.length() > 6000) {
            System.out.println("⚠️ 分析数据过长，进行智能裁剪: " + data.length() + " -> 6000字符");
            
            // 分析类型特定的裁剪策略
            if ("FINANCE".equals(analysisType) || "ORDER".equals(analysisType)) {
                // 保留摘要部分和关键指标
                int summaryEnd = data.indexOf("\n-");
                if (summaryEnd > 0 && summaryEnd < 500) {
                    // 提取摘要部分
                    String summary = data.substring(0, summaryEnd);
                    
                    // 提取关键指标 (通常是以"-"或"•"开头的行)
                    StringBuilder keyMetrics = new StringBuilder();
                    String[] lines = data.split("\n");
                    int metricsCount = 0;
                    
                    for (String line : lines) {
                        if ((line.trim().startsWith("-") || line.trim().startsWith("•")) 
                             && metricsCount < 20) {  // 最多保留20个关键指标
                            keyMetrics.append(line).append("\n");
                            metricsCount++;
                        }
                    }
                    
                    return summary + "\n" + keyMetrics.toString() + 
                           "\n(数据已优化处理以提高分析效率)";
                }
            }
            
            // 默认裁剪策略：保留开头、中间关键部分和结尾
            return data.substring(0, 2500) + 
                   "\n...(数据已优化)...\n" + 
                   data.substring(data.length() - 2500);
        }
        
        return data;
    }
    
    /**
     * 获取根据分析类型动态确定的超时时间
     */
    private int getAnalysisTimeout(String analysisType) {
        return switch (analysisType.toUpperCase()) {
            case "ORDER" -> ORDER_ANALYSIS_TIMEOUT;
            case "FINANCE" -> 750; // 财务分析较复杂，给75秒
            case "INVENTORY" -> 600; // 库存分析，标准60秒
            case "SALES" -> 600; // 销售分析，标准60秒
            default -> ANALYSIS_TIMEOUT; // 默认分析超时
        };
    }
    
    /**
     * 针对分析场景优化的重试机制
     */
    private String callAnalysisWithOptimizedRetry(String input, String systemPrompt, 
                                                int timeoutSeconds, String analysisType) throws IOException {
        int maxRetries = 3;
        long baseDelay = 1500; // 增加基础延迟
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                System.out.println(String.format("🔍 分析调用[%s] - 尝试%d/%d", analysisType, attempt, maxRetries));
                
                // 第一次尝试正常调用，第二次尝试简化提示词，第三次尝试降低生成长度
                if (attempt == 1) {
                    return callDeepSeekAPI(input, systemPrompt, timeoutSeconds);
                } else if (attempt == 2) {
                    // 简化提示词，减少对格式的要求
                    String simplifiedPrompt = simplifyAnalysisPrompt(systemPrompt);
                    return callDeepSeekAPI(input, simplifiedPrompt, timeoutSeconds + 15); // 增加超时
                } else {
                    // 最后一次尝试：降低回复复杂度，增加超时时间
                    String emergencyPrompt = "你是数据分析师。分析以下数据并提供简短清晰的见解，无需格式化：\n";
                    // 进一步压缩输入数据
                    String reducedInput = reduceInputSize(input);
                    return callDeepSeekAPI(reducedInput, emergencyPrompt, timeoutSeconds + 30); // 显著增加超时
                }
            } catch (IOException e) {
                System.err.println(String.format("❌ 分析失败[%s] - 尝试%d: %s", analysisType, attempt, e.getMessage()));
                
                if (attempt == maxRetries) {
                    // 返回基础分析结果而不是抛出异常
                    return generateBackupAnalysis(input, analysisType);
                }
                
                // 指数退避延迟
                try {
                    long delay = baseDelay * (1L << (attempt - 1)); // 1.5s, 3s, 6s
                    System.out.println(String.format("⏳ 等待%dms后重试...", delay));
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        // 最后的备用方案
        return "数据分析过程中遇到技术问题，请查看基础统计数据作为参考。";
    }
    
    /**
     * 简化分析提示词，降低格式要求
     */
    private String simplifyAnalysisPrompt(String originalPrompt) {
        // 保留核心指示，去除复杂的格式要求
        return "你是数据分析师。基于以下数据提供业务洞察和建议。\n" +
              "要点：\n" +
              "• 简明扼要分析关键趋势\n" +
              "• 提出2-3条具体可行的建议\n" +
              "• 回复控制在300-400字内";
    }
    
    /**
     * 进一步减少输入数据大小
     */
    private String reduceInputSize(String input) {
        if (input.length() <= 1500) {
            return input;
        }
        
        // 只保留前900和后600个字符
        return input.substring(0, 900) + "\n...[数据已大幅简化]...\n" + 
               input.substring(input.length() - 600);
    }
    
    /**
     * 生成备用分析结果
     * 当API调用全部失败时，提供基本的数值分析
     */
    private String generateBackupAnalysis(String input, String analysisType) {
        try {
            // 提取可能的数字数据
            List<Double> numbers = extractNumbers(input);
            
            StringBuilder result = new StringBuilder();
            result.append("🔢 基础数据统计 (AI深度分析暂不可用)\n\n");
            
            if (!numbers.isEmpty()) {
                // 计算基础统计数据
                double sum = 0, max = numbers.get(0), min = numbers.get(0);
                for (double num : numbers) {
                    sum += num;
                    max = Math.max(max, num);
                    min = Math.min(min, num);
                }
                double avg = sum / numbers.size();
                
                result.append("• 数据点数量: ").append(numbers.size()).append("\n");
                result.append("• 总和: ").append(String.format("%.2f", sum)).append("\n");
                result.append("• 平均值: ").append(String.format("%.2f", avg)).append("\n");
                result.append("• 最大值: ").append(String.format("%.2f", max)).append("\n");
                result.append("• 最小值: ").append(String.format("%.2f", min)).append("\n\n");
                
                // 简单趋势判断
                result.append("📈 简单趋势: ");
                if (numbers.size() >= 3) {
                    int last = numbers.size() - 1;
                    if (numbers.get(last) > numbers.get(last-1) && numbers.get(last-1) > numbers.get(last-2)) {
                        result.append("近期呈上升趋势");
                    } else if (numbers.get(last) < numbers.get(last-1) && numbers.get(last-1) < numbers.get(last-2)) {
                        result.append("近期呈下降趋势");
                    } else {
                        result.append("近期呈波动趋势");
                    }
                } else {
                    result.append("数据点不足，无法判断趋势");
                }
            } else {
                result.append("无法从输入中提取数值数据进行分析。");
            }
            
            return result.toString();
            
        } catch (Exception e) {
            return "数据分析服务暂时不可用，请稍后重试。";
        }
    }
    
    /**
     * 从文本中提取可能的数字
     */
    private List<Double> extractNumbers(String text) {
        List<Double> numbers = new ArrayList<>();
        
        // 简单正则匹配数字 (可能是整数或小数)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b\\d+(\\.\\d+)?\\b");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            try {
                double number = Double.parseDouble(matcher.group());
                // 过滤掉可能是年份、日期等的数字
                if (number > 0 && number < 1_000_000) {
                    numbers.add(number);
                }
            } catch (NumberFormatException ignored) {
                // 忽略解析错误
            }
        }
        
        return numbers;
    }

    /**
     * 进行订单数据分析
     * 改进: 增加超时控制和分批处理
     */
    public String analyzeOrderData(String orderData) {
        // 限制分析数据大小，防止请求过大
        String trimmedData = orderData;
        if (orderData.length() > 5000) {
            // 如果数据过长，保留关键部分
            System.out.println("⚠️ 订单分析数据过长，进行截断: " + orderData.length() + " -> 5000字符");
            trimmedData = orderData.substring(0, 2000) + 
                      "\n...(数据省略)...\n" +
                      orderData.substring(orderData.length() - 2000);
        }
        
        System.out.println("🧠 开始AI订单分析，优化后数据长度: " + trimmedData.length());
        
        // 构建提示词 - 针对订单分析进行优化
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", 
            "你是一位企业ERP系统的高级商业分析师，专长于订单数据分析。" +
            "根据提供的订单数据，提供清晰的业务洞察和切实可行的建议。" +
            "分析应包含：销售/采购趋势、客户分析、产品表现、利润分析和优化建议。" +
            "回复应简明扼要，突出关键指标和有针对性的改进点。"
        ));
        messages.add(Map.of("role", "user", "content", trimmedData));
        
        // 使用订单分析专用的超时设置
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 1200);
        requestBody.put("temperature", 0.4); // 降低温度以获得更专业的分析
        
        // 尝试执行分析，带重试逻辑
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                System.out.println("🧠 AI调用[ORDER_ANALYSIS] - 尝试" + attempt + "/" + maxAttempts);
                
                String analysis = executeApiCall(requestBody, ORDER_ANALYSIS_TIMEOUT);
                
                // 检查回复质量
                if (analysis != null && analysis.length() > 100) {
                    return analysis;
                }
                
                System.out.println("⚠️ AI分析回复质量不佳，准备重试");
                
            } catch (Exception e) {
                System.err.println("❌ AI分析请求失败 (尝试 " + attempt + "/" + maxAttempts + "): " + e.getMessage());
                if (attempt == maxAttempts) {
                    return "由于API限制，无法完成深度分析。请参考下方基础分析结果。";
                }
                
                // 等待后重试
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        return "抱歉，无法完成AI分析，请查看基础分析数据。";
    }

    /**
     * 自定义提示词模式 - 灵活调用
     */
    public String askWithCustomPrompt(String input, String systemPrompt) throws IOException {
        // 确保自定义提示词模式有合理的行为
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            return chat(input);  // 如果提示词为空，降级为普通对话
        }
        
        // 对长提示词进行智能裁剪
        String optimizedPrompt = optimizeSystemPrompt(systemPrompt);
        
        return callAIWithRetry(input, optimizedPrompt, CONVERSATION_TIMEOUT, "CUSTOM");
    }
    
    /**
     * 智能会话模式 - 同时处理ERP业务和通用知识
     * 最适合处理混合查询场景
     */
    public String smartChat(String input) throws IOException {
        String systemPrompt = buildSmartChatPrompt();
        return callAIWithRetry(input, systemPrompt, CONVERSATION_TIMEOUT, "SMART_CHAT");
    }

    /**
     * 优化系统提示词，避免过长
     */
    private String optimizeSystemPrompt(String prompt) {
        if (prompt == null) {
            return "";
        }
        
        // 如果提示词过长，进行智能裁剪
        if (prompt.length() > 1000) {
            // 取前700字符和后200字符，保留主要指令
            return prompt.substring(0, 700) + 
                   "\n...(内容已优化)...\n" + 
                   prompt.substring(prompt.length() - 200);
        }
        
        return prompt;
    }

    /**
     * 构建智能会话提示词 - 兼顾ERP功能和通用AI能力
     */
    private String buildSmartChatPrompt() {
        return "你是蘑菇头ERP系统的AI助手，名为「小蘑菇」。你拥有双重能力：\n\n" +
               "1. 作为ERP系统助手，你可以帮助用户处理以下业务功能：\n" +
               "   - 订单管理：创建、查询、修改、删除订单\n" +
               "   - 库存管理：查询库存、出入库操作\n" +
               "   - 财务分析：销售统计、利润分析\n" +
               "   - 客户管理：客户信息查询、历史订单\n\n" +
               "2. 作为通用AI助手，你可以回答各种知识问题，包括：\n" +
               "   - 百科知识、技术问题\n" +
               "   - 数学计算、文学创作\n" +
               "   - 提供建议、解释概念\n\n" +
               "你应该根据用户输入的内容，智能判断用户的意图：\n" +
               "- 如果是ERP系统相关问题，提供系统操作指导\n" +
               "- 如果是通用知识问题，直接回答\n" +
               "- 如有必要，可以主动询问用户需求以澄清\n\n" +
               "回答时保持专业、友好，语言简洁明了。";
    }

    /**
     * 带重试机制的AI调用
     */
    private String callAIWithRetry(String input, String systemPrompt, int timeoutSeconds, String mode) throws IOException {
        int maxRetries = 3;
        long baseDelay = 10000; // 1秒基础延迟
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                System.out.println(String.format("🤖 AI调用[%s] - 尝试%d/%d", mode, attempt, maxRetries));
                return callDeepSeekAPI(input, systemPrompt, timeoutSeconds);
                
            } catch (IOException e) {
                System.err.println(String.format("❌ AI调用失败[%s] - 尝试%d: %s", mode, attempt, e.getMessage()));
                
                if (attempt == maxRetries) {
                    throw new IOException(String.format("AI服务调用失败，已重试%d次：%s", maxRetries, e.getMessage()));
                }
                
                // 指数退避延迟
                try {
                    long delay = baseDelay * (1L << (attempt - 1)); // 1s, 2s, 4s
                    System.out.println(String.format("⏳ 等待%dms后重试...", delay));
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("重试被中断", ie);
                }
            }
        }
        throw new IOException("不应该到达这里");
    }

    /**
     * 核心AI API调用方法
     */
    private String callDeepSeekAPI(String input, String systemPrompt, int timeoutSeconds) throws IOException {
        OkHttpClient client = buildHttpClient(timeoutSeconds);
        
        Map<String, Object> payload = buildRequestPayload(input, systemPrompt);
        String requestBody = mapper.writeValueAsString(payload);

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                //.addHeader("User-Agent", "daacooerpERP/1.0")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorDetail = response.body() != null ? response.body().string() : "无详细错误信息";
                throw new IOException(String.format("API请求失败 [%d]: %s - %s", 
                    response.code(), response.message(), errorDetail));
            }

            String responseBody = response.body().string();
            return parseAIResponse(responseBody);
        }
    }

    /**
     * 构建HTTP客户端
     */
    private OkHttpClient buildHttpClient(int timeoutSeconds) {
        return new OkHttpClient.Builder()
                .connectTimeout(Math.min(timeoutSeconds / 2, 20), TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds + 10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(chain -> {
                    int maxRetries = 2;
                    int attempts = 0;
                    Request request = chain.request();
                    
                    IOException ioException = null;
                    while (attempts < maxRetries) {
                        try {
                            if (attempts > 0) {
                                System.out.println(String.format("🔄 HTTP请求重试 %d/%d: %s", 
                                    attempts + 1, maxRetries, request.url()));
                            }
                            return chain.proceed(request);
                        } catch (SocketTimeoutException e) {
                            ioException = e;
                            attempts++;
                            if (attempts >= maxRetries) break;
                            
                            long delay = 1000L * (1L << attempts);
                            try {
                                System.out.println(String.format("⏳ 连接超时, %dms后重试: %s", 
                                    delay, e.getMessage()));
                                Thread.sleep(delay);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                throw new IOException("重试被中断", ie);
                            }
                        } catch (IOException e) {
                            throw e;
                        }
                    }
                    
                    throw ioException != null ? ioException : 
                        new IOException("达到最大重试次数");
                })
                .build();
    }

    /**
     * 构建请求负载
     */
    private Map<String, Object> buildRequestPayload(String input, String systemPrompt) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "deepseek-chat");
        payload.put("temperature", 0.7); // 适中的创造性
        payload.put("max_tokens", 2000); // 限制响应长度
        payload.put("top_p", 0.9);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", input));
        payload.put("messages", messages);

        return payload;
    }

    /**
     * 解析AI响应
     */
    private String parseAIResponse(String responseBody) throws IOException {
        try {
            JsonNode root = mapper.readTree(responseBody);
            
            // 检查错误
            if (root.has("error")) {
                String errorMsg = root.path("error").path("message").asText("未知错误");
                throw new IOException("AI服务返回错误: " + errorMsg);
            }
            
            // 提取回复内容
            JsonNode choices = root.path("choices");
            if (choices.isEmpty()) {
                throw new IOException("AI响应中没有choices字段");
            }
            
            String content = choices.get(0).path("message").path("content").asText();
            if (content.isEmpty()) {
                throw new IOException("AI响应内容为空");
            }
            
            // 智能内容清理
            return cleanAIResponse(content);
            
        } catch (Exception e) {
            System.err.println("📄 AI原始响应: " + responseBody);
            throw new IOException("解析AI响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 智能清理AI响应内容
     */
    private String cleanAIResponse(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        String cleaned = content.trim();
        
        // 移除markdown代码块标记
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            int lastTripleBacktick = cleaned.lastIndexOf("```");
            if (firstNewline != -1 && lastTripleBacktick > firstNewline) {
                cleaned = cleaned.substring(firstNewline + 1, lastTripleBacktick).trim();
            }
        }
        
        // 移除markdown星号标记（粗体、斜体）
        cleaned = cleaned.replaceAll("\\*\\*([^*]+?)\\*\\*", "$1"); // 移除粗体 **text** -> text
        cleaned = cleaned.replaceAll("\\*([^*]+?)\\*", "$1");       // 移除斜体 *text* -> text
        
        // 递归处理多层嵌套的星号
        String previous;
        do {
            previous = cleaned;
            cleaned = cleaned.replaceAll("\\*\\*([^*]+?)\\*\\*", "$1");
            cleaned = cleaned.replaceAll("\\*([^*]+?)\\*", "$1");
        } while (!cleaned.equals(previous));
        
        // 如果是JSON格式，验证并格式化
        if (isJSONContent(cleaned)) {
            try {
                JsonNode json = mapper.readTree(cleaned);
                return mapper.writeValueAsString(json); // 标准化JSON格式
            } catch (Exception e) {
                System.out.println("⚠️ JSON格式化失败，返回原内容: " + e.getMessage());
            }
        }
        
        return cleaned;
    }

    /**
     * 判断内容是否为JSON格式
     */
    private boolean isJSONContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        String trimmed = content.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
               (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    /**
     * 构建对话提示词
     */
    private String buildChatPrompt() {
        return """
            你是蘑菇头ERP系统的AI助手，名字叫小蘑菇🍄。你的性格特点：
            
            🎯 核心特质:
            - 友好温馨，像贴心的小伙伴一样
            - 幽默风趣，但保持专业分寸
            - 主动帮助，善于理解用户真实需求
            - 简洁明了，避免冗长说教
            - 善于倾听，对用户的困难表示理解
            
            💼 专业能力:
            - ERP系统使用指导和最佳实践
            - 企业管理建议和流程优化
            - 业务数据分析和洞察解读
            - 供应链管理和库存优化
            - 财务管理和成本控制
            - 订单管理和客户关系
            
            💬 对话风格:
            - 🔥 热情主动: 主动询问需要什么帮助，积极提供解决方案
            - 😊 亲切自然: 使用温暖的语言，让用户感到轻松舒适
            - 🎯 精准高效: 快速理解问题核心，给出针对性建议
            - 💡 启发思考: 不只给答案，还解释原因和最佳实践
            - 🤝 感同身受: 理解用户的业务压力和挑战
            
            📚 智能响应策略:
            • 业务咨询: 提供实用的管理建议和操作指导
            • 技术支持: 解释系统功能，指导正确使用方法
            • 数据分析: 帮助解读报表，发现业务洞察
            • 流程优化: 建议改进业务流程，提高效率
            • 问题解决: 协助排查问题，提供多种解决方案
            
            🌟 特殊场景处理:
            - 订单管理问题：耐心指导，提供最佳实践
            - 系统操作疑惑：详细解释，避免用户迷茫
            - 业务流程困惑：分步骤指导，确保理解
            - 数据异常情况：冷静分析，给出排查方向
            - 紧急业务需求：快速响应，优先解决
            
            🎨 回复格式:
            - 适当使用emoji增加亲和力 (不要过多)
            - 重要信息直接表达，不要用星号粗体标记
            - 步骤用数字或bullet points清晰展示
            - 根据问题复杂度调整回复长度
            - 结尾主动询问是否需要更多帮助
            - 不要使用markdown格式如**粗体**等
            
            💝 温馨提醒:
            - 始终站在用户角度思考问题
            - 对不确定的信息要诚实说明
            - 遇到复杂问题时，建议分步骤处理
            - 保持耐心，即使是重复性问题
            - 记住你是用户可信赖的业务伙伴
            - 回复使用纯文本格式，避免markdown标记
            
            请用温暖专业的语调与用户交流，让每一次对话都成为愉快的体验！🌟
            """;
    }

    /**
     * 构建意图识别提示词
     */
    private String buildIntentPrompt() {
        return """
            你是智能意图识别专家。分析用户输入，判断其真实意图。
            
            🎯 **识别类型:**
            1. **COMMAND** - 要求执行具体系统操作
               - 关键词：创建、查询、删除、修改、统计、导出等
               - 示例：「创建订单」「查询销售额」「删除库存」
               - 🆕 价格补充：「单价5元」「每个3元」「一瓶5元」等价格信息也属于COMMAND
            
            2. **CONVERSATION** - 日常对话交流
               - 关键词：问候、感谢、询问、闲聊、求助等  
               - 示例：「你好」「谢谢」「今天天气」「你是谁」
            
            3. **MIXED** - 既有操作需求又有对话元素
               - 示例：「你好，帮我查一下订单」「麻烦创建个订单，谢谢」
            
            🔍 **特殊识别规则:**
            - 含有价格信息的短语（如「单价X元」「每个X元」「一瓶X元」「价格X」）都应识别为COMMAND
            - 纯数字+单位+货币（如「5元/个」「3块钱」）也应识别为COMMAND
            - 这些通常是对之前订单创建请求的价格补充信息
            
            📊 **返回格式 (严格JSON):**
            {
              "intent_type": "COMMAND/CONVERSATION/MIXED",
              "confidence": 0.0-1.0,
              "command": "提取的核心操作指令(仅COMMAND/MIXED)",
              "reasoning": "判断依据(简短说明)"
            }
            
            🚨 **重要**: 只返回JSON，不要任何额外文字！
            """;
    }

    /**
     * 构建智能指令解析提示词
     */
    private String buildCommandPrompt() {
        return """
            你是智能ERP指令解析器。从用户输入中提取信息，转换为标准JSON。
            
            🎯 **解析规则（按优先级）:**
            1. 识别操作类型：
               - 🔥 含有"卖给"、"卖给了"、"卖了"、"出售"、"售给"等动词 → create_order
               - 含有"买"、"购买"、"采购"、"进货"等动词 → create_order  
               - 含有"查询"、"查看"、"查找"等动词 → query_order
               - 含有"删除"、"取消"等动词 → delete_order
               - 含有"分析"、"统计"等动词 → analyze_order
            2. 识别订单类型：采购关键词→PURCHASE，销售关键词→SALE，默认SALE
            3. 提取客户/供应商：匹配"为[姓名]"、"给[姓名]"、"从[姓名]"、"向[姓名]"等
            4. 提取商品：匹配商品名称+数量+价格的组合模式
            5. 智能推断缺失信息：缺价格设为0，但不要自动填充客户名或商品信息
            6. 保留原始输入：将用户的原始输入添加到original_input字段
            
            📦 **订单类型识别:**
            • **PURCHASE(采购)**: 采购、进货、购买、进料、补货、订购、从供应商、向厂家、从XX那里买、买了、购买了、从XX买、从XX购买
            • **SALE(销售)**: 销售、出售、卖给、卖给了、卖了、售给、发货、交付、为客户、给客户、出售给
            
            🚨 **重要：采购识别优先级**
            ⚠️ 特别注意："从XX买了50台电脑" → 这是采购订单(PURCHASE)，不是销售订单！
            ⚠️ 特别注意："从冯天祎那里买了1台笔记本电脑" → 这是采购订单(PURCHASE)，不是销售订单！
            ⚠️ 关键模式："从[任何人名/公司名]买/购买/采购" → 必须识别为 PURCHASE
            ⚠️ 关键模式："从[任何人名/公司名]那里买/购买/采购" → 必须识别为 PURCHASE  
            ⚠️ 销售模式："卖给[客户]/为[客户]" → 识别为 SALE
            
            🔴 **采购关键模式识别 - 优先级最高：**
            • "从XX买" → PURCHASE
            • "从XX那里买" → PURCHASE  
            • "从XX这里买" → PURCHASE
            • "从XX处买" → PURCHASE
            • "从XX购买" → PURCHASE
            • "从XX采购" → PURCHASE
            • "向XX买" → PURCHASE
            • "和XX买" → PURCHASE
            
            📝 **解析示例（严格按此格式）:**
            
            ===== 🔵 销售订单示例 =====
            输入："创建订单"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "", "products": [], "original_input": "创建订单"}
            
            输入："为张三创建销售订单，苹果10个单价5元"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "张三", "products": [{"name": "苹果", "quantity": 10, "unit_price": 5.0}], "original_input": "为张三创建销售订单，苹果10个单价5元"}
            
            输入："卖给李四20个橙子每个3元"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "李四", "products": [{"name": "橙子", "quantity": 20, "unit_price": 3.0}], "original_input": "卖给李四20个橙子每个3元"}
            
            输入："卖给了冯天祎三瓶水"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "冯天祎", "products": [{"name": "水", "quantity": 3, "unit_price": 0}], "original_input": "卖给了冯天祎三瓶水"}
            
            输入："发货给王五，香蕉15个单价2元"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "王五", "products": [{"name": "香蕉", "quantity": 15, "unit_price": 2.0}], "original_input": "发货给王五，香蕉15个单价2元"}
            
            输入："卖了5个苹果给张三"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "张三", "products": [{"name": "苹果", "quantity": 5, "unit_price": 0}], "original_input": "卖了5个苹果给张三"}
            
            输入："出售给客户王五10瓶饮料"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "王五", "products": [{"name": "饮料", "quantity": 10, "unit_price": 0}], "original_input": "出售给客户王五10瓶饮料"}
            
            ===== 🟠 采购订单示例 =====
            输入："创建采购订单"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "", "products": [], "original_input": "创建采购订单"}
            
            输入："从供应商张三采购苹果100个单价3元"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "张三", "products": [{"name": "苹果", "quantity": 100, "unit_price": 3.0}], "original_input": "从供应商张三采购苹果100个单价3元"}
            
            输入："向厂家进货橙子200个每个2.5元"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "厂家", "products": [{"name": "橙子", "quantity": 200, "unit_price": 2.5}], "original_input": "向厂家进货橙子200个每个2.5元"}
            
            输入："从哈振宇那里买了5瓶水，一瓶3元"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "哈振宇", "products": [{"name": "水", "quantity": 5, "unit_price": 3.0}], "original_input": "从哈振宇那里买了5瓶水，一瓶3元"}
            
            输入："从冯天祎那里买了1台笔记本电脑，每台1000元"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "冯天祎", "products": [{"name": "笔记本电脑", "quantity": 1, "unit_price": 1000.0}], "original_input": "从冯天祎那里买了1台笔记本电脑，每台1000元"}
            
            输入："从李老板那里采购大米50袋单价80元"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "李老板", "products": [{"name": "大米", "quantity": 50, "unit_price": 80.0}], "original_input": "从李老板那里采购大米50袋单价80元"}
            
            输入："购买原料，大米50袋单价80元"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "", "products": [{"name": "大米", "quantity": 50, "unit_price": 80.0}], "original_input": "购买原料，大米50袋单价80元"}
            
            输入："补货梨子30个价格4元"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "", "products": [{"name": "梨子", "quantity": 30, "unit_price": 4.0}], "original_input": "补货梨子30个价格4元"}
            
            ===== 🆕 自然语言表达示例 =====
            输入："从王小明那里买10瓶饮料每瓶5块钱"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "王小明", "products": [{"name": "饮料", "quantity": 10, "unit_price": 5.0}], "original_input": "从王小明那里买10瓶饮料每瓶5块钱"}
            
            输入："给客户刘大海发货，苹果20个一个3.5元"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "刘大海", "products": [{"name": "苹果", "quantity": 20, "unit_price": 3.5}], "original_input": "给客户刘大海发货，苹果20个一个3.5元"}
            
            输入："和张师傅订了30斤大米每斤6元"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "张师傅", "products": [{"name": "大米", "quantity": 30, "unit_price": 6.0}], "original_input": "和张师傅订了30斤大米每斤6元"}
            
            输入："帮李阿姨买香蕉15个单价2块"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "李阿姨", "products": [{"name": "香蕉", "quantity": 15, "unit_price": 2.0}], "original_input": "帮李阿姨买香蕉15个单价2块"}
            
            ===== 🔍 查询示例 =====
            输入："查询王五的订单"
            输出：{"action": "query_order", "customer": "王五", "original_input": "查询王五的订单"}
            
            输入："查询采购订单"
            输出：{"action": "query_order", "order_type": "PURCHASE", "original_input": "查询采购订单"}
            
            输入："查询销售订单"
            输出：{"action": "query_order", "order_type": "SALE", "original_input": "查询销售订单"}
            
            输入："删除订单123"
            输出：{"action": "delete_order", "order_id": 123, "original_input": "删除订单123"}
            
            ===== 📊 分析示例 =====
            输入："分析这些订单"
            输出：{"action": "analyze_order", "original_input": "分析这些订单"}
            
            输入："分析订单数据"
            输出：{"action": "analyze_order", "original_input": "分析订单数据"}
            
            输入："帮我分析一下订单情况"
            输出：{"action": "analyze_order", "original_input": "帮我分析一下订单情况"}
            
            输入："订单分析"
            输出：{"action": "analyze_order", "original_input": "订单分析"}
            
            输入："分析张三的订单"
            输出：{"action": "analyze_order", "customer": "张三", "original_input": "分析张三的订单"}
            
            输入："分析销售订单"
            输出：{"action": "analyze_order", "order_type": "SALE", "original_input": "分析销售订单"}
            
            ===== 🆕 缺失信息处理示例 =====
            输入："创建订单，苹果10个单价5元"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "", "products": [{"name": "苹果", "quantity": 10, "unit_price": 5.0}], "original_input": "创建订单，苹果10个单价5元"}
            
            输入："为张三创建订单"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "张三", "products": [], "original_input": "为张三创建订单"}
            
            输入："客户是李四"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "李四", "products": [], "original_input": "客户是李四"}
            
            输入："苹果10个单价5元"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "", "products": [{"name": "苹果", "quantity": 10, "unit_price": 5.0}], "original_input": "苹果10个单价5元"}
            
            输入："补充客户信息：王五"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "王五", "products": [], "original_input": "补充客户信息：王五"}
            
            ===== 💰 纯价格补充示例 =====
            输入："单价5元"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "", "products": [{"name": "", "quantity": 0, "unit_price": 5.0}], "original_input": "单价5元"}
            
            输入："每瓶5元"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "", "products": [{"name": "", "quantity": 0, "unit_price": 5.0}], "original_input": "每瓶5元"}
            
            输入："一个3元"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "", "products": [{"name": "", "quantity": 0, "unit_price": 3.0}], "original_input": "一个3元"}
            
            输入："价格4块钱"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "", "products": [{"name": "", "quantity": 0, "unit_price": 4.0}], "original_input": "价格4块钱"}
            
            输入："3元/个"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "", "products": [{"name": "", "quantity": 0, "unit_price": 3.0}], "original_input": "3元/个"}
            
            🔧 **提取技巧:**
            - 订单类型：优先检查采购关键词（从XX买、采购、进货），再检查销售关键词，默认销售
            - 客户/供应商：在"为/给/从/向/和/跟"后面，或"的"前面，"那里/这里/处"前面
            - 商品名：常见中文词汇（水果、食品、用品、原料、水、饮料等）
            - 数量：数字+个/件/只/袋/箱/瓶/斤等单位，或"数量X"
            - 单价：数字+元/块/钱等，或"单价/每个/一个/一瓶/价格X"
            - 多商品用逗号分隔解析
            
            🚨 **严格要求:**
            1. 只返回JSON，不要解释文字
            2. JSON格式必须标准，可直接解析
            3. 宁可字段为空也不要缺失必需字段
            4. 数字类型用数值，文本用字符串
            5. 订单类型必须是"SALE"或"PURCHASE"
            6. 客户名可以是任何中文或英文姓名
            7. 必须包含original_input字段记录原始输入
            """;
    }

    /**
     * 构建分析提示词
     */
    private String buildAnalysisPrompt(String analysisType) {
        String basePrompt = """
            你是专业的商业数据分析师。基于提供的数据进行深度分析。
            
            ⚠️ 重要格式要求:
            - 严禁使用 ** 星号粗体标记
            - 严禁使用任何markdown格式
            - 标题用emoji前缀，不要加星号
            - 内容直接表达，不要包围星号
            
            📊 分析要求:
            - 数据洞察要准确客观
            - 趋势判断要有依据
            - 建议要切实可行
            - 风险提示要明确
            
            💡 输出格式示例:
            🎯 关键指标总结
            • 数据项1: 具体数值
            • 数据项2: 具体数值
            
            📈 趋势分析
            • 趋势1: 简要说明
            • 趋势2: 简要说明
            
            🚀 行动建议
            • 建议1: 具体措施
            • 建议2: 具体措施
            
            请严格按照示例格式，绝不使用星号标记！
            """;
        
        return switch (analysisType.toUpperCase()) {
            case "FINANCE" -> basePrompt + "\n🏦 专注领域: 财务健康度、现金流、盈利能力分析\n请按照上述格式要求输出，不要有星号！";
            case "SALES" -> basePrompt + "\n📈 专注领域: 销售业绩、客户分析、市场趋势\n请按照上述格式要求输出，不要有星号！";
            case "INVENTORY" -> basePrompt + "\n📦 专注领域: 库存优化、周转率、供应链效率\n请按照上述格式要求输出，不要有星号！";
            case "ORDER" -> basePrompt + "\n📋 专注领域: 订单流程、客户满意度、运营效率\n请按照上述格式要求输出，不要有星号！";
            default -> basePrompt + "\n🔍 专注领域: 综合业务分析\n请按照上述格式要求输出，不要有星号！";
        };
    }

    /**
     * 构建专门的订单分析提示词 - 优化性能
     */
    private String buildOrderAnalysisPrompt() {
        return """
            你是高效的订单数据分析师。快速分析订单数据，生成简洁有用的洞察报告。
            
            ⚠️ 格式要求 - 严格遵守:
            - 绝对不要使用 ** 星号标记
            - 绝对不要使用任何markdown格式
            - 标题直接写，不要加粗体标记
            - 重要内容用emoji前缀，不要用星号包围
            
            🎯 分析重点:
            - 📊 订单概况: 总量、类型分布、状态概览
            - 💰 金额分析: 销售额、采购额、盈利情况
            - 👥 客户洞察: 主要客户、订单频率
            - 📈 趋势判断: 业务增长、模式识别
            - ⚠️ 风险提示: 异常情况、注意事项
            
            📋 输出示例格式:
            🎯 核心指标
            • 订单总数: 16个
            • 销售订单: 6个 | 采购订单: 10个
            
            💡 业务洞察  
            • 采购密集期，可能在备货
            • 客户分布良好，风险分散
            
            🚀 优化建议
            • 及时处理待确认订单
            • 关注现金流动情况
            
            ❌ 禁止格式: **标题**、**重点内容**等任何星号标记
            ✅ 正确格式: 直接写标题，用emoji区分层级
            
            请严格按照示例格式输出，不要有任何星号！
            """;
    }

    /**
     * 健康检查方法
     */
    public boolean healthCheck() {
        try {
            String testPrompt = "你好，这是一个连接测试。请简短回复'连接正常'。";
            String response = askWithCustomPrompt("测试", testPrompt);
            return response.contains("连接正常") || response.length() > 0;
        } catch (Exception e) {
            System.err.println("❌ AI服务健康检查失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取服务状态信息
     */
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "DeepSeek AI");
        status.put("endpoint", API_URL);
        status.put("timestamp", new Date());
        
        try {
            long startTime = System.currentTimeMillis();
            boolean healthy = healthCheck();
            long responseTime = System.currentTimeMillis() - startTime;
            
            status.put("healthy", healthy);
            status.put("responseTime", responseTime + "ms");
            status.put("status", healthy ? "ACTIVE" : "INACTIVE");
        } catch (Exception e) {
            status.put("healthy", false);
            status.put("status", "ERROR");
            status.put("error", e.getMessage());
        }
        
        return status;
    }

    /**
     * 兼容旧方法 - 标准请求
     */
    @Deprecated
    public String ask(String prompt) throws IOException {
        return parseCommand(prompt);
    }

    /**
     * 执行API调用 - 通用方法
     */
    private String executeApiCall(Map<String, Object> requestBody, int timeoutSeconds) throws IOException {
        // 序列化请求体
        String jsonBody = mapper.writeValueAsString(requestBody);
        
        // 构建请求
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();
                
        // 构建HTTP客户端
        OkHttpClient client = buildHttpClient(timeoutSeconds);
        
        // 执行请求
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("API调用失败，HTTP错误: " + response.code() + ", " + errorBody);
            }
            
            String responseBody = response.body().string();
            JsonNode responseJson = mapper.readTree(responseBody);
            
            // 提取回复内容
            if (responseJson.has("choices") && responseJson.get("choices").isArray() && 
                responseJson.get("choices").size() > 0) {
                
                JsonNode firstChoice = responseJson.get("choices").get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    return firstChoice.get("message").get("content").asText();
                }
            }
            
            throw new IOException("无法从API响应中提取回复内容");
        }
    }

    /**
     * 生成本地分析结果
     * 当API服务不可用或超时时，提供基本的本地数据分析
     * 
     * @param input 用户输入的文本
     * @param dataContext 数据上下文信息
     * @param analysisType 分析类型
     * @return 本地生成的分析结果
     */
    public String generateLocalAnalysis(String input, String dataContext, String analysisType) {
        try {
            StringBuilder result = new StringBuilder();
            result.append("本地分析结果 (AI服务暂时不可用)\n\n");
            
            // 根据分析类型提供不同的分析逻辑
            switch (analysisType.toUpperCase()) {
                case "ORDER":
                    result.append("订单分析：\n");
                    // 提取可能的订单数据
                    if (dataContext != null && !dataContext.isEmpty()) {
                        // 从dataContext中提取订单信息
                        result.append(analyzeOrderContext(dataContext));
                    } else {
                        result.append("• 未提供足够的订单数据进行分析\n");
                    }
                    break;
                    
                case "FINANCE":
                    result.append("财务分析：\n");
                    if (dataContext != null && !dataContext.isEmpty()) {
                        // 从dataContext中提取财务信息
                        result.append(analyzeFinanceContext(dataContext));
                    } else {
                        result.append("• 未提供足够的财务数据进行分析\n");
                    }
                    break;
                    
                case "INVENTORY":
                    result.append("库存分析：\n");
                    if (dataContext != null && !dataContext.isEmpty()) {
                        // 从dataContext中提取库存信息
                        result.append(analyzeInventoryContext(dataContext));
                    } else {
                        result.append("• 未提供足够的库存数据进行分析\n");
                    }
                    break;
                    
                default:
                    // 默认基础分析
                    result.append("基础数据分析：\n");
                    // 提取可能的数字数据
                    List<Double> numbers = extractNumbers(dataContext != null ? dataContext : input);
                    
                    if (!numbers.isEmpty()) {
                        // 计算基础统计数据
                        double sum = 0, max = numbers.get(0), min = numbers.get(0);
                        for (double num : numbers) {
                            sum += num;
                            max = Math.max(max, num);
                            min = Math.min(min, num);
                        }
                        double avg = sum / numbers.size();
                        
                        result.append("• 数据点数量: ").append(numbers.size()).append("\n");
                        result.append("• 总和: ").append(String.format("%.2f", sum)).append("\n");
                        result.append("• 平均值: ").append(String.format("%.2f", avg)).append("\n");
                        result.append("• 最大值: ").append(String.format("%.2f", max)).append("\n");
                        result.append("• 最小值: ").append(String.format("%.2f", min)).append("\n");
                    } else {
                        result.append("• 未能从提供的数据中提取有效的数值信息\n");
                    }
            }
            
            // 添加用户查询的基础解释
            if (input != null && !input.isEmpty()) {
                result.append("\n针对您的问题 \"").append(input).append("\"：\n");
                result.append("• 您可以查看上述基础统计数据作为参考\n");
                result.append("• 系统当前无法提供深入的AI分析，请稍后重试\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            return "生成本地分析时出错: " + e.getMessage() + "\n请稍后重试或联系系统管理员。";
        }
    }
    
    /**
     * 分析订单上下文数据
     */
    private String analyzeOrderContext(String orderContext) {
        StringBuilder analysis = new StringBuilder();
        
        // 尝试计算订单相关的简单统计
        try {
            // 计算订单数量
            int orderCount = countOccurrences(orderContext, "订单编号");
            if (orderCount > 0) {
                analysis.append("• 订单总数: ").append(orderCount).append("\n");
            }
            
            // 估算订单金额
            List<Double> amounts = extractNumbersFollowingPattern(orderContext, "金额[:：]\\s*([\\d\\.]+)");
            if (!amounts.isEmpty()) {
                double totalAmount = amounts.stream().mapToDouble(Double::doubleValue).sum();
                double avgAmount = totalAmount / amounts.size();
                analysis.append("• 估算订单总金额: ").append(String.format("%.2f", totalAmount)).append("\n");
                analysis.append("• 平均订单金额: ").append(String.format("%.2f", avgAmount)).append("\n");
            }
            
            // 简单状态统计
            int completedOrders = countOccurrences(orderContext, "已完成");
            int pendingOrders = countOccurrences(orderContext, "待处理");
            int cancelledOrders = countOccurrences(orderContext, "已取消");
            
            if (completedOrders > 0 || pendingOrders > 0 || cancelledOrders > 0) {
                analysis.append("• 订单状态分布:\n");
                if (completedOrders > 0) analysis.append("  - 已完成: ").append(completedOrders).append("\n");
                if (pendingOrders > 0) analysis.append("  - 待处理: ").append(pendingOrders).append("\n");
                if (cancelledOrders > 0) analysis.append("  - 已取消: ").append(cancelledOrders).append("\n");
            }
        } catch (Exception e) {
            analysis.append("• 订单数据分析出错: ").append(e.getMessage()).append("\n");
        }
        
        if (analysis.length() == 0) {
            analysis.append("• 未能从提供的数据中提取有效的订单信息\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * 分析财务上下文数据
     */
    private String analyzeFinanceContext(String financeContext) {
        StringBuilder analysis = new StringBuilder();
        
        try {
            // 提取收入和支出相关数据
            List<Double> incomes = extractNumbersFollowingPattern(financeContext, "收入[:：]\\s*([\\d\\.]+)");
            List<Double> expenses = extractNumbersFollowingPattern(financeContext, "支出[:：]\\s*([\\d\\.]+)");
            
            if (!incomes.isEmpty()) {
                double totalIncome = incomes.stream().mapToDouble(Double::doubleValue).sum();
                analysis.append("• 总收入: ").append(String.format("%.2f", totalIncome)).append("\n");
            }
            
            if (!expenses.isEmpty()) {
                double totalExpense = expenses.stream().mapToDouble(Double::doubleValue).sum();
                analysis.append("• 总支出: ").append(String.format("%.2f", totalExpense)).append("\n");
            }
            
            if (!incomes.isEmpty() && !expenses.isEmpty()) {
                double totalIncome = incomes.stream().mapToDouble(Double::doubleValue).sum();
                double totalExpense = expenses.stream().mapToDouble(Double::doubleValue).sum();
                double profit = totalIncome - totalExpense;
                analysis.append("• 估算利润: ").append(String.format("%.2f", profit)).append("\n");
            }
        } catch (Exception e) {
            analysis.append("• 财务数据分析出错: ").append(e.getMessage()).append("\n");
        }
        
        if (analysis.length() == 0) {
            analysis.append("• 未能从提供的数据中提取有效的财务信息\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * 分析库存上下文数据
     */
    private String analyzeInventoryContext(String inventoryContext) {
        StringBuilder analysis = new StringBuilder();
        
        try {
            // 计算产品数量
            int productCount = countOccurrences(inventoryContext, "产品编号");
            if (productCount > 0) {
                analysis.append("• 产品种类数: ").append(productCount).append("\n");
            }
            
            // 提取库存数量
            List<Double> quantities = extractNumbersFollowingPattern(inventoryContext, "数量[:：]\\s*([\\d\\.]+)");
            if (!quantities.isEmpty()) {
                double totalQuantity = quantities.stream().mapToDouble(Double::doubleValue).sum();
                double avgQuantity = totalQuantity / quantities.size();
                analysis.append("• 总库存数量: ").append(String.format("%.0f", totalQuantity)).append("\n");
                analysis.append("• 平均每种产品库存: ").append(String.format("%.2f", avgQuantity)).append("\n");
            }
            
            // 低库存警告
            List<Double> lowStocks = extractNumbersFollowingPattern(inventoryContext, "库存不足|库存紧张");
            if (!lowStocks.isEmpty()) {
                analysis.append("• 有").append(lowStocks.size()).append("种产品库存不足，需要补货\n");
            }
        } catch (Exception e) {
            analysis.append("• 库存数据分析出错: ").append(e.getMessage()).append("\n");
        }
        
        if (analysis.length() == 0) {
            analysis.append("• 未能从提供的数据中提取有效的库存信息\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * 计算字符串中特定模式出现的次数
     */
    private int countOccurrences(String text, String pattern) {
        if (text == null || pattern == null) return 0;
        
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
    
    /**
     * 提取跟随特定模式后的数字
     */
    private List<Double> extractNumbersFollowingPattern(String text, String regex) {
        List<Double> numbers = new ArrayList<>();
        if (text == null || regex == null) return numbers;
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            try {
                String numStr = matcher.group(1);
                numbers.add(Double.parseDouble(numStr));
            } catch (Exception ignored) {
                // 忽略无法解析的数字
            }
        }
        
        return numbers;
    }

    /**
     * 从文本中检测订单类型
     */
    private String detectOrderTypeFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        // 🚨 采购关键词 - 优先级更高，因为销售是默认
        // 特别注意"从XX买"这种常见表达
        String[] purchaseKeywords = {
            "采购", "进货", "购买", "进料", "补货", "订购", "进仓", "入库",
            "从供应商", "向厂家", "向供应商", "从厂家", "供应商", "厂家", 
            "批发", "进购", "采买", "购进", "收货", "进材料", "买材料",
            "从.*买", "从.*购买", "从.*采购", "从.*进货",  // 🆕 关键修复：从XX买的模式
            "向.*买", "向.*购买", "向.*采购", "向.*进货"   // 🆕 向XX买的模式
        };
        
        // 使用正则表达式检查采购模式
        for (String keyword : purchaseKeywords) {
            if (keyword.contains(".*")) {
                // 对于包含正则的关键词，使用正则匹配
                if (text.matches(".*" + keyword + ".*")) {
                    System.out.println("🛒 检测到采购模式: " + keyword + " 在文本: " + text);
                    return "PURCHASE";
                }
            } else {
                // 对于普通关键词，使用包含检查
                if (text.contains(keyword)) {
                    System.out.println("🛒 检测到采购关键词: " + keyword);
                    return "PURCHASE";
                }
            }
        }
        
        // 销售关键词
        String[] saleKeywords = {
            "销售", "出售", "卖给", "售给", "发货", "交付", "为客户", "给客户",
            "销", "卖", "售", "出货", "零售", "批售", "出售给", "卖出",
            "客户订单", "销售订单", "出库", "发给"
        };
        
        for (String keyword : saleKeywords) {
            if (text.contains(keyword)) {
                System.out.println("💰 检测到销售关键词: " + keyword);
                return "SALE";
            }
        }
        
        return ""; // 无法确定
    }
} 