package com.daacooerp.erp.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.daacooerp.erp.service.AIService;
import com.daacooerp.erp.service.CommandExecutorService;
import com.daacooerp.erp.dto.AIRequest;
import com.daacooerp.erp.dto.AIResponse;
import com.daacooerp.erp.service.external.DeepSeekAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 智能AI服务实现类
 * 支持自然对话和智能指令执行
 */
@Service
public class AIServiceImpl implements AIService {

    @Autowired
    private CommandExecutorService commandExecutor;

    @Autowired
    private DeepSeekAIService deepSeekAIService;

    private final ObjectMapper mapper = new ObjectMapper();

    // 真正需要确认的危险操作（大幅减少）
    private static final Set<String> DANGEROUS_ACTIONS = Set.of(
        "delete_order"  // 只有删除操作需要确认
    );

    // 操作描述映射
    private static final Map<String, String> ACTION_DESCRIPTIONS = Map.of(
        "create_order", "创建新订单",
        "delete_order", "删除订单",
        "confirm_order", "确认订单",
        "query_order", "查询订单信息",
        "query_sales", "查询销售数据",
        "query_inventory", "查询库存信息",
        "analyze_finance", "财务数据分析",
        "analyze_order", "订单数据分析"
    );

    // 添加会话管理
    private final Map<String, Long> sessionTimestamps = new HashMap<>();
    private static final long SESSION_TIMEOUT = 5 * 60 * 1000; // 5分钟会话超时

    @Override
    public AIResponse parseAndExecute(String input, boolean confirmed) {
        try {
            System.out.println("🎯 处理用户输入: " + input + " (已确认: " + confirmed + ")");
            
            // 显式检测是否指向通用AI能力的请求
            if (isGeneralAIQuery(input) && !confirmed) {
                System.out.println("🧠 检测到通用AI问答请求，直接使用对话模式");
                return handleConversation(input);
            }
            
            // 第一步：智能意图识别
            IntentResult intent = analyzeIntent(input);
            
            System.out.println("🎯 意图识别结果：" + intent.type + " (置信度: " + intent.confidence + ")");
            
            switch (intent.type) {
                case COMMAND:
                    return handleCommand(input, intent.extractedCommand, confirmed);
                case CONVERSATION:
                    return handleConversation(input);
                case MIXED:
                    return handleMixedIntent(input, intent.extractedCommand, confirmed);
                default:
                    return handleConversation(input); // 默认当作对话处理
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return new AIResponse("😅 抱歉，我遇到了一些问题：" + e.getMessage(), false);
        }
    }

    @Override
    public AIResponse getBusinessInsights(AIRequest request) {
        try {
            String analysisType = request.getAnalysisType() != null ? request.getAnalysisType() : "GENERAL";
            String dataContext = request.getDataContext() != null ? request.getDataContext() : "";
            
            String insight = deepSeekAIService.analyzeData(
                request.getInput() + "\n" + dataContext, 
                analysisType
            );
            
            return new AIResponse("📊 " + insight, false);
            
        } catch (Exception e) {
            e.printStackTrace();
            return new AIResponse("😅 业务洞察分析失败：" + e.getMessage(), false);
        }
    }

    /**
     * 智能意图识别
     */
    private IntentResult analyzeIntent(String input) {
        try {
            String response = deepSeekAIService.analyzeIntent(input);
            
            System.out.println("🔍 意图分析原始回复：" + response);
            
            // 解析意图分析结果
            JsonNode result = mapper.readTree(response);
            String type = result.path("intent_type").asText("CONVERSATION");
            double confidence = result.path("confidence").asDouble(0.5);
            String extractedCommand = result.path("command").asText("");
            
            return new IntentResult(
                IntentType.valueOf(type.toUpperCase()), 
                confidence, 
                extractedCommand
            );
            
        } catch (Exception e) {
            System.out.println("⚠️ 意图识别失败，使用智能规则判断：" + e.getMessage());
            return fallbackIntentAnalysis(input);
        }
    }

    /**
     * 备用意图分析（基于关键词规则）
     */
    private IntentResult fallbackIntentAnalysis(String input) {
        String lowerInput = input.toLowerCase();
        
        // ERP指令关键词 - 更全面的业务关键词列表
        String[] erpKeywords = {
            // 订单操作
            "创建订单", "新订单", "下单", "采购", "销售", "出售", "买", "卖", "供应商", "客户订单",
            "删除订单", "取消订单", "订单查询", "查询订单", "确认订单", "完成订单",
            // 库存操作
            "库存", "入库", "出库", "盘点", "商品", "产品", "材料",
            // 财务操作
            "财务", "金额", "账单", "收款", "付款", "报表", "利润", "成本",
            // 分析操作
            "统计数据", "分析订单", "分析销售", "分析趋势"
        };
        
        // 快速识别常用指令模式
        if (lowerInput.contains("分析") && (lowerInput.contains("订单") || lowerInput.contains("这些"))) {
            System.out.println("🎯 快速识别: 订单分析指令");
            return new IntentResult(IntentType.COMMAND, 0.95, "分析订单");
        }
        
        // ERP业务相关检测
        boolean isErpCommand = Arrays.stream(erpKeywords)
            .anyMatch(keyword -> lowerInput.contains(keyword));
        
        // 通用指令关键词
        String[] generalCommandKeywords = {"创建", "查询", "删除", "修改", "统计", "分析", "导出", "确认", "添加"};
        boolean hasCommandKeyword = Arrays.stream(generalCommandKeywords)
            .anyMatch(keyword -> lowerInput.contains(keyword));
        
        // 对话关键词
        String[] conversationKeywords = {
            "你好", "谢谢", "再见", "怎么样", "是什么", "为什么", "你能", "能不能",
            "？", "帮我", "请问", "如何", "怎么", "帮我", "认为", "觉得", "聊聊"
        };
        boolean hasConversationKeyword = Arrays.stream(conversationKeywords)
            .anyMatch(keyword -> lowerInput.contains(keyword));
        
        // 当确定是ERP指令
        if (isErpCommand) {
            return new IntentResult(IntentType.COMMAND, 0.95, input);
        }
        // 混合意图检测
        else if (hasCommandKeyword && hasConversationKeyword) {
            return new IntentResult(IntentType.MIXED, 0.8, input);
        } 
        // 可能是一般指令
        else if (hasCommandKeyword) {
            return new IntentResult(IntentType.COMMAND, 0.7, input);
        } 
        // 默认为对话
        else {
            return new IntentResult(IntentType.CONVERSATION, 0.8, "");
        }
    }

    /**
     * 智能处理系统指令
     */
    private AIResponse handleCommand(String input, String extractedCommand, boolean confirmed) {
        try {
            System.out.println("🎮 开始处理指令，原始输入：" + input);
            
            // 将自然语言转换为结构化指令
            String commandInput = !extractedCommand.isEmpty() ? extractedCommand : input;
            String jsonCommand = deepSeekAIService.parseCommand(commandInput);
            
            System.out.println("🎮 AI生成的JSON指令：" + jsonCommand);
            
            // 解析并验证JSON指令
            JsonNode commandNode;
            try {
                commandNode = mapper.readTree(jsonCommand);
            } catch (Exception e) {
                System.out.println("❌ JSON解析失败，尝试修复...");
                // 尝试修复常见的JSON格式问题
                String fixedJson = fixJsonFormat(jsonCommand);
                commandNode = mapper.readTree(fixedJson);
                System.out.println("✅ JSON修复成功：" + fixedJson);
            }
            
            String action = commandNode.path("action").asText();
            
            if (action.isEmpty()) {
                // 如果无法识别为系统指令，尝试当作通用对话处理
                System.out.println("⚠️ 无法识别操作类型，尝试作为普通对话处理");
                return handleConversation(input);
            }
            
            // 增强JSON节点信息（添加原始输入和会话ID便于调试）
            if (commandNode instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) commandNode)
                    .put("original_input", input);
                
                // 为订单创建操作添加会话ID，确保上下文能够共享
                if ("create_order".equals(action)) {
                    // 生成或使用现有的会话ID
                    String sessionId = generateSessionId(input);
                    ((com.fasterxml.jackson.databind.node.ObjectNode) commandNode)
                        .put("session_id", sessionId);
                    System.out.println("🔗 设置会话ID: " + sessionId);
                }
            }
            
            // 危险操作确认（仅删除操作需要确认）
            if (isDangerous(action) && !confirmed) {
                String confirmMessage = generateSimpleConfirmMessage(action, commandNode, input);
                return new AIResponse(confirmMessage, true);
            }
            
            // 🆕 如果用户已经确认，直接执行，不再进行额外检查
            if (confirmed) {
                System.out.println("✅ 用户已确认，直接执行指令: " + action);
            }
            
            // 执行指令
            System.out.println("🚀 执行指令: " + action);
            String result = commandExecutor.execute(commandNode);
            
            // 智能结果处理
            if (result == null || result.trim().isEmpty()) {
                result = "✅ 操作已完成";
            }
            
            // 🔧 智能检测是否为确认信息（需要用户确认）
            // 分析类操作不需要确认
            boolean isAnalysisAction = "analyze_order".equals(action) || 
                                     "analyze_finance".equals(action) || 
                                     "query_sales".equals(action) ||
                                     "query_inventory".equals(action);
            
            boolean isConfirmationMessage = !isAnalysisAction && isConfirmationMessage(result);
            
            if (isConfirmationMessage && !confirmed) {
                // 这是确认信息，需要用户确认
                System.out.println("📋 检测到确认信息，等待用户确认");
                return new AIResponse(result, true); // needConfirm = true
            }
            
            // 生成增强的友好回复
            String enhancedResponse = generateEnhancedResponse(result, action, commandNode, input);
            return new AIResponse(enhancedResponse, false);
            
        } catch (Exception e) {
            System.err.println("❌ 指令处理失败：" + e.getMessage());
            e.printStackTrace();
            
            // 指令处理失败时，尝试降级为普通对话
            if (e.getMessage() != null && (
                e.getMessage().contains("无法解析") || 
                e.getMessage().contains("未知操作") || 
                e.getMessage().contains("无法识别"))) {
                System.out.println("🔄 降级为普通对话模式");
                try {
                    return handleConversation(input);
                } catch (Exception chatError) {
                    // 如果对话处理也失败，返回错误响应
                    return generateErrorResponse(e, input);
                }
            }
            
            // 根据错误类型提供更精准的帮助
            return generateErrorResponse(e, input);
        }
    }

    /**
     * 修复JSON格式问题
     */
    private String fixJsonFormat(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return "{}";
        }
        
        String fixed = jsonStr.trim();
        
        // 移除markdown标记
        if (fixed.startsWith("```")) {
            fixed = fixed.replaceAll("```[a-zA-Z]*", "").replaceAll("```", "").trim();
        }
        
        // 确保是有效的JSON对象
        if (!fixed.startsWith("{")) {
            fixed = "{" + fixed;
        }
        if (!fixed.endsWith("}")) {
            fixed = fixed + "}";
        }
        
        // 修复常见的JSON问题
        fixed = fixed.replace("'", "\""); // 单引号改双引号
        fixed = fixed.replaceAll("([{,]\\s*)([a-zA-Z_][a-zA-Z0-9_]*)(\\s*:)", "$1\"$2\"$3"); // 没有引号的键名
        
        return fixed;
    }

    /**
     * 生成简洁确认消息
     */
    private String generateSimpleConfirmMessage(String action, JsonNode commandNode, String originalInput) {
        if ("delete_order".equals(action)) {
            long orderId = commandNode.path("order_id").asLong(0);
            return String.format("🗑️ 确认删除订单 %d？\n\n⚠️ 删除后无法恢复\n\n回复'是'确认，'否'取消", orderId);
        }
        
        // 其他操作的简单确认
        return String.format("⚠️ 确认执行：%s？\n\n回复'是'确认，'否'取消", getActionDescription(action));
    }

    /**
     * 生成增强的响应消息
     */
    private String generateEnhancedResponse(String result, String action, JsonNode commandNode, String originalInput) {
        // 如果执行结果已经很完善，直接返回
        if (result.contains("✅") || result.contains("❌") || result.length() > 50) {
            return result;
        }
        
        // 否则生成增强回复
        StringBuilder response = new StringBuilder();
        
        String emoji = getActionEmoji(action);
        String description = getActionDescription(action);
        
        response.append(emoji).append(" ").append(description).append("完成\n\n");
        response.append(result);
        
        // 添加相关建议
        appendRelatedSuggestions(response, action);
        
        return response.toString();
    }

    /**
     * 添加相关操作建议
     */
    private void appendRelatedSuggestions(StringBuilder response, String action) {
        response.append("\n\n💡 您还可以：\n");
        
        switch (action) {
            case "create_order":
                response.append("• 查询刚创建的订单\n• 确认订单并设置运费\n• 查看今日订单统计");
                break;
            case "query_order":
                response.append("• 查询销售数据\n• 分析订单趋势\n• 导出订单报表");
                break;
            case "query_sales":
                response.append("• 查看详细订单\n• 分析客户数据\n• 生成销售报告");
                break;
            default:
                response.append("• 继续其他操作\n• 查看系统帮助");
        }
    }

    /**
     * 生成错误响应
     */
    private AIResponse generateErrorResponse(Exception e, String input) {
        String errorMsg = e.getMessage() != null ? e.getMessage() : "未知错误";
        
        StringBuilder response = new StringBuilder();
        response.append("😅 处理过程中遇到问题：\n\n");
        
        // 根据错误类型提供针对性建议
        if (errorMsg.contains("JSON")) {
            response.append("🔧 **解决建议：**\n");
            response.append("• 请尝试更简单的表达\n");
            response.append("• 确保包含必要信息（如客户名、商品名）\n");
            response.append("• 例如：'为张三创建订单，苹果10个，单价5元'\n");
        } else if (errorMsg.contains("timeout") || errorMsg.contains("连接")) {
            response.append("🌐 **网络问题：**\n");
            response.append("• 请稍后重试\n");
            response.append("• 检查网络连接\n");
        } else {
            response.append("🛠️ **通用建议：**\n");
            response.append("• 重新整理表达方式\n");
            response.append("• 确保信息完整清晰\n");
            response.append("• 可以先尝试简单操作\n");
        }
        
        response.append("\n💬 您的输入：").append(input);
        response.append("\n🔧 技术细节：").append(errorMsg);
        
        return new AIResponse(response.toString(), false);
    }

    /**
     * 处理对话
     */
    private AIResponse handleConversation(String input) {
        try {
            System.out.println("💬 处理普通对话：" + input);
            
            // 使用新的智能对话模式，能同时处理ERP相关问题和通用知识
            String response = deepSeekAIService.smartChat(input);
            
            // 如果智能对话返回为空，使用增强提示方式
            if (response == null || response.trim().isEmpty()) {
                // 增强对话体验 - 添加ERP系统上下文
                String enhancedPrompt = String.format(
                    "我是蘑菇头ERP系统的AI助手，除了能够帮用户处理ERP系统中的订单、库存、财务等业务操作外，" +
                    "也能回答各种通用知识问题。用户的问题是：%s\n\n" +
                    "如果这是关于ERP系统的问题，我会提供相关帮助；如果是通用知识问题，我会直接回答。", 
                    input
                );
                
                // 调用通用对话API
                response = deepSeekAIService.askWithCustomPrompt(input, enhancedPrompt);
            }
            
            // 兜底：如果前两种方式失败，使用普通对话API
            if (response == null || response.trim().isEmpty()) {
                response = deepSeekAIService.chat(input);
            }
            
            return new AIResponse(response, false);
        } catch (Exception e) {
            e.printStackTrace();
            return new AIResponse("😅 对话处理出错：" + e.getMessage(), false);
        }
    }

    /**
     * 处理混合意图
     */
    private AIResponse handleMixedIntent(String input, String extractedCommand, boolean confirmed) {
        try {
            System.out.println("🔄 处理混合意图：" + input);
            
            // 先处理指令部分
            AIResponse commandResult = handleCommand(input, extractedCommand, confirmed);
            
            if (commandResult.isNeedConfirm()) {
                return commandResult; // 需要确认时直接返回
            }
            
            // 再生成对话式的友好回复
            String contextPrompt = String.format(
                "用户说：%s\n执行结果：%s\n\n请生成一个自然友好的回复，既确认操作结果，又体现对话的温暖感。回复要简洁不啰嗦。",
                input, commandResult.getReply()
            );
            
            String friendlyResponse = deepSeekAIService.askWithCustomPrompt(contextPrompt,
                "你是友好的AI助手小蘑菇。将操作结果包装成自然对话式的回复，保持轻松友好的语调。");
            
            return new AIResponse(friendlyResponse, false);
            
        } catch (Exception e) {
            e.printStackTrace();
            // 如果混合处理失败，尝试退回到纯对话模式
            System.out.println("🔄 混合意图处理失败，退回至对话模式");
            return handleConversation(input);
        }
    }

    /**
     * 判断是否为危险操作
     */
    private boolean isDangerous(String action) {
        return DANGEROUS_ACTIONS.contains(action);
    }

    /**
     * 生成确认消息
     */
    private String generateConfirmMessage(String action, JsonNode commandNode) {
        String actionDesc = getActionDescription(action);
        StringBuilder confirmMsg = new StringBuilder();
        
        confirmMsg.append("🤔 检测到敏感操作：").append(actionDesc).append("\n\n");
        
        // 根据不同操作类型添加具体信息
        switch (action) {
            case "create_order":
                String customer = commandNode.path("customer").asText("未指定客户");
                confirmMsg.append("📝 将要创建订单：\n");
                confirmMsg.append("• 客户：").append(customer).append("\n");
                JsonNode products = commandNode.path("products");
                if (products.isArray() && products.size() > 0) {
                    confirmMsg.append("• 商品数量：").append(products.size()).append("种\n");
                }
                break;
                
            case "delete_order":
                String orderId = commandNode.path("order_id").asText();
                if (!orderId.isEmpty()) {
                    confirmMsg.append("🗑️ 将要删除订单ID：").append(orderId).append("\n");
                }
                break;
                
            case "confirm_order":
                String confirmOrderId = commandNode.path("order_id").asText();
                double freight = commandNode.path("freight").asDouble(0);
                confirmMsg.append("✅ 将要确认订单：\n");
                if (!confirmOrderId.isEmpty()) {
                    confirmMsg.append("• 订单ID：").append(confirmOrderId).append("\n");
                }
                if (freight > 0) {
                    confirmMsg.append("• 运费：").append(freight).append("元\n");
                }
                break;
        }
        
        confirmMsg.append("\n🚨 此操作不可撤销，确定要继续吗？");
        return confirmMsg.toString();
    }

    /**
     * 获取操作描述
     */
    private String getActionDescription(String action) {
        return ACTION_DESCRIPTIONS.getOrDefault(action, "未知操作");
    }

    /**
     * 生成友好的操作结果回复
     */
    private String generateFriendlyResponse(String result, String action, JsonNode commandNode) {
        String emoji = getActionEmoji(action);
        
        // 如果结果已经很友好了，直接返回
        if (result.contains("✅") || result.contains("❌") || result.contains("📊")) {
            return result;
        }
        
        // 否则添加emoji和友好语调
        StringBuilder response = new StringBuilder();
        response.append(emoji).append(" ");
        
        switch (action) {
            case "create_order":
                response.append("订单创建成功！\n").append(result);
                break;
            case "query_order":
                response.append("为您查询到以下订单信息：\n").append(result);
                break;
            case "delete_order":
                response.append("订单删除完成。\n").append(result);
                break;
            case "confirm_order":
                response.append("订单确认成功！\n").append(result);
                break;
            case "query_sales":
                response.append("销售数据查询结果：\n").append(result);
                break;
            case "query_inventory":
                response.append("库存信息如下：\n").append(result);
                break;
            default:
                response.append(result);
        }
        
        return response.toString();
    }

    /**
     * 获取操作对应的emoji
     */
    private String getActionEmoji(String action) {
        return switch (action) {
            case "create_order" -> "📝";
            case "query_order" -> "🔍";
            case "delete_order" -> "🗑️";
            case "confirm_order" -> "✅";
            case "query_sales" -> "💰";
            case "query_inventory" -> "📦";
            case "analyze_finance" -> "📊";
            default -> "🤖";
        };
    }

    /**
     * 生成会话ID，用于维护对话上下文
     */
    private String generateSessionId(String input) {
        // 清理过期会话
        cleanupExpiredSessions();
        
        // 对于订单创建，使用统一的会话ID前缀，在短时间内共享上下文
        String sessionPrefix = "order_creation";
        long currentTime = System.currentTimeMillis();
        
        // 检查是否有活跃的订单创建会话
        for (Map.Entry<String, Long> entry : sessionTimestamps.entrySet()) {
            if (entry.getKey().startsWith(sessionPrefix) && 
                (currentTime - entry.getValue()) < SESSION_TIMEOUT) {
                // 更新时间戳并复用会话
                sessionTimestamps.put(entry.getKey(), currentTime);
                System.out.println("🔄 复用现有会话: " + entry.getKey());
                return entry.getKey();
            }
        }
        
        // 创建新会话
        String newSessionId = sessionPrefix + "_" + (currentTime / 1000);
        sessionTimestamps.put(newSessionId, currentTime);
        System.out.println("🆕 创建新会话: " + newSessionId);
        return newSessionId;
    }
    
    /**
     * 清理过期会话
     */
    private void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        sessionTimestamps.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > SESSION_TIMEOUT);
    }

    /**
     * 🔧 智能检测是否为确认信息
     * 用于判断CommandExecutor返回的结果是确认信息还是执行完成信息
     */
    private boolean isConfirmationMessage(String result) {
        if (result == null || result.trim().isEmpty()) {
            return false;
        }
        
        String lowerResult = result.toLowerCase();
        
        // 首先排除明显是分析结果的情况
        if ((lowerResult.contains("分析报告") || lowerResult.contains("数据分析") || 
             lowerResult.contains("核心指标") || lowerResult.contains("业务洞察")) &&
            (lowerResult.contains("订单总数") || lowerResult.contains("销售订单") ||
             lowerResult.contains("采购订单") || lowerResult.contains("金额") ||
             lowerResult.contains("优化建议"))) {
            System.out.println("🔍 检测到分析结果，不需要确认");
            return false;
        }
        
        // 确认信息的典型特征
        String[] confirmationPatterns = {
            "请确认", "确认创建", "确认订单", "confirm", 
            "💬 确认", "回复：'确认'", "回复'确认'",
            "📋 请确认", "💵 总金额：", "商品明细："
        };
        
        // 检查是否包含确认关键词
        for (String pattern : confirmationPatterns) {
            if (lowerResult.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        
        // 检查是否包含"确认"+"订单"的组合
        if (lowerResult.contains("确认") && lowerResult.contains("订单")) {
            return true;
        }
        
        // 检查是否包含价格明细格式（通常出现在确认信息中）
        if (lowerResult.contains("¥") && lowerResult.contains("×") && lowerResult.contains("@")) {
            return true;
        }
        
        return false;
    }

    /**
     * 检测是否是请求通用AI能力的问题
     * 用于识别明确与ERP系统无关的问题
     */
    private boolean isGeneralAIQuery(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String lowerInput = input.toLowerCase();
        
        // 通用知识性问题关键词
        String[] generalKnowledgeKeywords = {
            "什么是", "如何实现", "怎么做", "介绍一下", "解释", "定义", 
            "历史", "原理", "方法", "区别", "比较", "教程", "讲解",
            "写一篇", "生成", "创作", "编写", "设计", "总结", "推荐"
        };
        
        // 问题性表达
        String[] questionPatterns = {
            "能不能", "可以吗", "如何", "为什么", "是什么", "在哪里", 
            "什么时候", "怎样", "有哪些", "告诉我", "知道", "请介绍"
        };
        
        // 检测是否是一般性问题
        for (String keyword : generalKnowledgeKeywords) {
            if (lowerInput.contains(keyword)) {
                // 同时检查是否包含ERP相关词汇
                if (!containsERPTerms(lowerInput)) {
                    return true;
                }
            }
        }
        
        // 检查问题模式
        for (String pattern : questionPatterns) {
            if (lowerInput.contains(pattern)) {
                // 排除明显的ERP相关问题
                if (!containsERPTerms(lowerInput)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查文本是否包含ERP相关术语
     */
    private boolean containsERPTerms(String text) {
        String[] erpTerms = {
            "订单", "客户", "供应商", "商品", "价格", "销售", "采购", 
            "库存", "入库", "出库", "账单", "财务", "报表", "erp", "系统"
        };
        
        for (String term : erpTerms) {
            if (text.contains(term)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 意图识别结果内部类
     */
    private static class IntentResult {
        public final IntentType type;
        public final double confidence;
        public final String extractedCommand;

        public IntentResult(IntentType type, double confidence, String extractedCommand) {
            this.type = type;
            this.confidence = confidence;
            this.extractedCommand = extractedCommand;
        }
    }

    /**
     * 意图类型枚举
     */
    private enum IntentType {
        COMMAND,     // 纯指令执行
        CONVERSATION, // 纯对话交流
        MIXED        // 混合意图
    }
} 