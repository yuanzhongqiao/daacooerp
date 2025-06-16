package com.daacooerp.erp.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.daacooerp.erp.entity.Goods;
import com.daacooerp.erp.entity.Order;
import com.daacooerp.erp.entity.OrderGoods;
import com.daacooerp.erp.service.OrderService;
import com.daacooerp.erp.service.CommandExecutorService;
import com.daacooerp.erp.service.external.DeepSeekAIService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * 命令执行服务实现类
 * 负责处理各种业务命令的执行
 */
@Service
public class CommandExecutorServiceImpl implements CommandExecutorService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private DeepSeekAIService deepSeekAIService;

    private final ObjectMapper mapper = new ObjectMapper();
    
    // 对话上下文缓存，用于保存订单创建过程中的信息
    private final Map<String, OrderContext> orderContextCache = new ConcurrentHashMap<>();
    
    // 对话上下文超时时间（毫秒）
    private static final long CONTEXT_TIMEOUT = 10 * 60 * 1000; // 10分钟
    
    // 智能学习缓存 - 记住用户的习惯表达
    private final Map<String, String> customerAliasCache = new ConcurrentHashMap<>(); // 客户别名映射
    private final Map<String, String> productAliasCache = new ConcurrentHashMap<>(); // 商品别名映射
    private final Map<String, Float> productPriceCache = new ConcurrentHashMap<>(); // 商品常用价格
    private final Map<String, CustomerPreference> customerPreferenceCache = new ConcurrentHashMap<>(); // 客户偏好

    @Override
    public String execute(JsonNode root) {
        String action = root.path("action").asText();
        
        System.out.println("🎮 执行指令: " + action + " - " + root.toString());
        
        // 添加会话ID支持
        String sessionId = root.has("session_id") ? root.get("session_id").asText() : "";
        
        // 🧠 智能识别不同类型的用户输入
        String originalInput = root.has("original_input") ? root.get("original_input").asText() : "";
        
        // 确保确认指令能够正确传递会话ID
        // 1. 处理确认指令
        if (isConfirmationInput(originalInput)) {
            // 增加日志追踪会话处理
            System.out.println("🔄 检测到确认输入，会话ID: " + sessionId);
            if (hasIncompleteOrderContext(sessionId)) {
                System.out.println("✅ 找到未完成订单上下文，准备确认订单");
                return handleOrderConfirmation(sessionId);
            } else {
                System.out.println("❌ 未找到相关订单上下文，无法确认");
            }
        }
        
        // 2. 处理修改指令
        if (isModificationInput(originalInput) && hasIncompleteOrderContext(sessionId)) {
            return handleOrderModification(root, sessionId);
        }
        
        // 3. 处理纯价格输入的特殊情况
        if (isPriceOnlyInput(originalInput) && hasIncompleteOrderContext(sessionId)) {
            return handlePriceCompletion(root, sessionId);
        }

        return switch (action) {
            case "create_order" -> handleCreateOrder(root, sessionId);
            case "delete_order" -> handleDeleteOrder(root);
            case "query_order" -> handleQueryOrder(root);
            case "confirm_order" -> handleConfirmOrder(root);
            case "query_sales" -> handleQuerySales(root);
            case "query_inventory" -> handleQueryInventory(root);
            case "analyze_finance" -> handleAnalyzeFinance(root);
            case "analyze_order" -> handleAnalyzeOrder(root);
            default -> "❓ 未知操作类型：" + action + "\n\n💡 支持的操作：\n• create_order (创建订单)\n• query_order (查询订单)\n• delete_order (删除订单)\n• confirm_order (确认订单)\n• query_sales (销售查询)\n• query_inventory (库存查询)\n• analyze_finance (财务分析)\n• analyze_order (订单分析)";
        };
    }
    
    /**
     * 🧠 智能识别确认输入
     */
    private boolean isConfirmationInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String[] confirmPatterns = {
            "确认", "对的", "是的", "好的", "没问题", "可以", "同意", "正确",
            "ok", "yes", "y", "好", "对", "是", "👍", "✅", "确定"
        };
        
        String lowerInput = input.toLowerCase().trim();
        for (String pattern : confirmPatterns) {
            if (lowerInput.equals(pattern) || lowerInput.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 🧠 智能识别修改输入
     */
    private boolean isModificationInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String[] modifyPatterns = {
            "改为", "修改", "改成", "变成", "换成", "不对", "错了", "应该是",
            "客户改", "价格改", "数量改", "商品改", "改一下", "更正"
        };
        
        String lowerInput = input.toLowerCase();
        for (String pattern : modifyPatterns) {
            if (lowerInput.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 🧠 处理订单确认
     */
    private String handleOrderConfirmation(String sessionId) {
        OrderContext context = getOrderContext(sessionId);
        if (context == null) {
            return "❌ 找不到待确认的订单信息，请重新开始创建订单";
        }
        
        // 最终验证信息完整性
        String validation = validateOrderContext(context);
        if (!validation.isEmpty()) {
            return "❌ 订单信息不完整：\n" + validation;
        }
        
        // 记录实际确认的供应商信息，防止被覆盖
        String confirmedSupplier = context.getCustomerName();
        System.out.println("✅ 订单确认: 确认供应商/客户名称为: " + confirmedSupplier);
        
        // 执行订单创建
        return completeOrderCreation(context, sessionId);
    }
    
    /**
     * 🧠 处理订单修改
     */
    private String handleOrderModification(JsonNode root, String sessionId) {
        OrderContext context = getOrderContext(sessionId);
        if (context == null) {
            return "❌ 找不到待修改的订单信息，请重新开始创建订单";
        }
        
        String originalInput = root.has("original_input") ? root.get("original_input").asText() : "";
        
        // 智能解析修改内容
        if (originalInput.contains("客户") && (originalInput.contains("改为") || originalInput.contains("改成"))) {
            String newCustomer = extractModificationValue(originalInput, "客户");
            if (!newCustomer.isEmpty()) {
                context.customerName = intelligentErrorCorrection(newCustomer, "customer");
                context.addClarification("客户修改为：" + context.customerName);
            }
        }
        
        if ((originalInput.contains("价格") || originalInput.contains("单价")) && 
            (originalInput.contains("改为") || originalInput.contains("改成"))) {
            float newPrice = extractPriceFromModification(originalInput);
            if (newPrice > 0 && !context.getProductList().isEmpty()) {
                context.getProductList().get(0).unitPrice = newPrice; // 简化：修改第一个商品的价格
                context.addClarification("价格修改为：¥" + newPrice);
            }
        }
        
        // 重新生成确认信息
        return generateSmartConfirmation(context);
    }
    
    /**
     * 🧠 提取修改值
     */
    private String extractModificationValue(String input, String field) {
        String pattern = field + "\\s*(?:改为|改成|是|为)\\s*([\\u4e00-\\u9fa5a-zA-Z0-9]+)";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(input);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "";
    }
    
    /**
     * 🧠 从修改指令中提取价格
     */
    private float extractPriceFromModification(String input) {
        String[] patterns = {
            "(?:价格|单价)\\s*(?:改为|改成|是)\\s*(\\d+(?:\\.\\d+)?)\\s*元?",
            "(\\d+(?:\\.\\d+)?)\\s*元"
        };
        
        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(input);
            if (m.find()) {
                try {
                    return Float.parseFloat(m.group(1));
                } catch (NumberFormatException e) {
                    // 继续尝试下一个模式
                }
            }
        }
        return 0;
    }
    
    /**
     * 🧠 验证订单上下文完整性
     */
    private String validateOrderContext(OrderContext context) {
        List<String> errors = new ArrayList<>();
        
        if (context.getCustomerName().isEmpty()) {
            errors.add("缺少客户信息");
        }
        
        if (context.getProductList().isEmpty()) {
            errors.add("缺少商品信息");
        } else {
            for (ProductInfo product : context.getProductList()) {
                if (product.name.isEmpty()) {
                    errors.add("商品名称不能为空");
                }
                if (product.quantity <= 0) {
                    errors.add("商品数量必须大于0");
                }
                if (product.unitPrice <= 0) {
                    errors.add("商品单价必须大于0");
                }
            }
        }
        
        return String.join("、", errors);
    }

    /**
     * 处理价格补充完成
     */
    private String handlePriceCompletion(JsonNode root, String sessionId) {
        // 提取价格和可能的商品信息
        String input = root.has("original_input") ? root.get("original_input").asText() : "";
        
        // 尝试先从输入中提取完整的商品信息（包括价格）
        ProductInfo completeProductInfo = extractProductFromText(input);
        
        // 如果提取到了完整商品信息，优先使用
        if (completeProductInfo != null && completeProductInfo.unitPrice > 0) {
            // 获取上下文
            OrderContext context = getOrderContext(sessionId);
            if (context == null) {
                return "❌ 无法找到未完成的订单创建请求，请重新开始创建订单";
            }
            
            // 更新或添加商品信息
            List<ProductInfo> products = context.getProductList();
            if (!products.isEmpty()) {
                // 更新现有商品的价格，如果商品名匹配的话
                boolean updated = false;
                for (ProductInfo product : products) {
                    if (product.name.equals(completeProductInfo.name) || 
                        (product.name.isEmpty() && completeProductInfo.name.equals("水"))) {
                        product.name = completeProductInfo.name;
                        product.quantity = completeProductInfo.quantity > 0 ? completeProductInfo.quantity : product.quantity;
                        product.unitPrice = completeProductInfo.unitPrice;
                        updated = true;
                        break;
                    }
                }
                if (!updated) {
                    // 如果没有匹配的商品，添加新商品
                    products.add(completeProductInfo);
                }
            } else {
                // 如果没有商品列表，直接添加
                products.add(completeProductInfo);
            }
            
            // 完成订单创建
            return completeOrderCreation(context, sessionId);
        }
        
        // 否则按照原来的逻辑处理纯价格信息
        float price = extractPriceOnly(input);
        
        if (price <= 0) {
            return "❌ 无法识别有效的价格信息，请重新输入（例如：'单价5元'）";
        }
        
        // 获取上下文
        OrderContext context = getOrderContext(sessionId);
        if (context == null) {
            return "❌ 无法找到未完成的订单创建请求，请重新开始创建订单";
        }
        
        // 更新商品价格
        for (ProductInfo product : context.getProductList()) {
            if (product.unitPrice <= 0) {
                product.unitPrice = price;
            }
        }
        
        // 完成订单创建
        return completeOrderCreation(context, sessionId);
    }
    
    /**
     * 完成订单创建
     */
    private String completeOrderCreation(OrderContext context, String sessionId) {
        try {
            // 创建订单对象
            Order order = new Order();
            order.setOrderType(context.getOrderType());
            
            // 确保使用正确的客户/供应商名称
            String customerName = context.getCustomerName();
            // 防止被历史数据覆盖
            if (customerName == null || customerName.isEmpty()) {
                System.out.println("⚠️ 警告: 客户/供应商名称为空，尝试恢复...");
                // 尝试从原始输入中重新提取
                if (context.getOriginalInput() != null && !context.getOriginalInput().isEmpty()) {
                    String extractedName = extractCustomerFromText(context.getOriginalInput());
                    if (extractedName != null && !extractedName.isEmpty()) {
                        customerName = extractedName;
                        System.out.println("✅ 成功恢复客户/供应商名称: " + customerName);
                    }
                }
            }
            
            order.setCustomerName(customerName);
            order.setCreatedAt(LocalDateTime.now());

            List<OrderGoods> goodsList = new ArrayList<>();
            float totalAmount = 0;
            int totalItems = 0;

            // 处理商品列表
            for (ProductInfo product : context.getProductList()) {
                // 验证产品信息
                if (product.quantity <= 0) {
                    return String.format("❌ 商品'%s'的数量无效\n💡 请提供正确的数量信息", product.name);
                }
                
                // 验证价格信息
                if (product.unitPrice <= 0) {
                    String priceType = order.getOrderType().equals("PURCHASE") ? "采购" : "销售";
                    return String.format("❌ 商品'%s'的%s单价无效\n💡 请提供正确的价格信息，例如：'%s单价5元'", 
                        product.name, priceType, product.name);
                }

                // 创建商品和订单商品关联
                Goods goods = new Goods();
                goods.setName(product.name);

                OrderGoods orderGoods = new OrderGoods();
                orderGoods.setGoods(goods);
                orderGoods.setQuantity(product.quantity);
                orderGoods.setUnitPrice(product.unitPrice);
                orderGoods.setTotalPrice(product.unitPrice * product.quantity);

                goodsList.add(orderGoods);
                totalAmount += orderGoods.getTotalPrice();
                totalItems += product.quantity;
            }

            order.setAmount(totalAmount);
            Order savedOrder = orderService.createOrder(order, goodsList);

            // 🆕 移除自动确认，让用户手动控制确认过程
            // 订单创建成功，但需要用户手动确认才会更新库存和财务记录
            
            // 生成简洁智能回复
            String orderTypeDesc = order.getOrderType().equals("PURCHASE") ? "采购" : "销售";
            String partnerLabel = order.getOrderType().equals("PURCHASE") ? "供应商" : "客户";
            String typeIcon = order.getOrderType().equals("PURCHASE") ? "📦" : "💰";
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("✅ %s%s订单创建成功！\n\n", typeIcon, orderTypeDesc));
            result.append(String.format("📋 订单号：%s | %s：%s | 金额：¥%.2f\n", 
                savedOrder.getOrderNo(), partnerLabel, order.getCustomerName(), totalAmount));
            
            // 简化的商品明细
            result.append(String.format("📦 商品：%d种/%d件", goodsList.size(), totalItems));
            if (goodsList.size() <= 2) {
                result.append(" (");
                for (int i = 0; i < goodsList.size(); i++) {
                    ProductInfo product = context.getProductList().get(i);
                    result.append(product.name).append("×").append(product.quantity);
                    if (i < goodsList.size() - 1) result.append(", ");
                }
                result.append(")");
            }
            result.append("\n\n⚠️ 订单状态：待确认 (PENDING)");
            result.append("\n💡 需要手动确认订单才会更新库存和财务记录");
            result.append("\n💡 可以说'查询订单").append(savedOrder.getOrderNo()).append("'查看详情");
            
            // 🧠 学习客户偏好（在订单成功创建后）
            learnCustomerPreference(order.getCustomerName(), context.getProductList(), order.getOrderType());
            
            // 清除上下文
            removeOrderContext(sessionId);
            
            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 创建订单失败：" + e.getMessage() + 
                "\n\n💡 请尝试更清晰的表达，如：'为张三创建订单，商品苹果10个单价5元'";
        }
    }

    /**
     * 🧠 超级智能创建订单 - 融合AI推理、学习记忆、容错纠正
     */
    private String handleCreateOrder(JsonNode root, String sessionId) {
        try {
            System.out.println("🧠 启动超智能订单分析: " + root.toString());
            
            // 🔍 检查是否为确认执行（用户已确认）
            boolean isConfirmedExecution = false;
            
            // 方法1：检查是否有会话上下文且信息完整
            OrderContext existingContext = getOrderContext(sessionId);
            if (existingContext != null) {
                String validation = validateOrderContext(existingContext);
                if (validation.isEmpty()) {
                    // 上下文完整，说明这是确认执行
                    isConfirmedExecution = true;
                    System.out.println("✅ 检测到完整上下文，直接执行订单创建");
                    return completeOrderCreation(existingContext, sessionId);
                }
            }
            
            // 🔍 第一步：基础信息提取
            String originalInput = root.has("original_input") ? root.get("original_input").asText() : "";
            String orderType = smartExtractOrderType(root);
            String customerName = smartExtractCustomer(root);
            List<ProductInfo> productList = smartExtractProducts(root);
            
            // 🔧 第二步：智能纠错和优化
            customerName = intelligentErrorCorrection(customerName, "customer");
            for (ProductInfo product : productList) {
                product.name = intelligentErrorCorrection(product.name, "product");
            }
            
            // 📋 第三步：创建订单上下文
            OrderContext context = new OrderContext(orderType, customerName, productList);
            context.setOriginalInput(originalInput);
            
            // 🧠 第四步：AI智能推理补全信息
            context = smartEngine.smartInferMissingInfo(context, originalInput);
            
            // 💾 第五步：保存上下文
            saveOrderContext(sessionId, context);
            
            // 🤔 第六步：检查是否还有缺失信息
            String missingInfoQuestion = detectMissingInfoAndAsk(context);
            if (!missingInfoQuestion.isEmpty()) {
                // 添加智能建议
                String suggestions = generateSmartSuggestions(context);
                if (!suggestions.isEmpty()) {
                    return missingInfoQuestion + "\n" + suggestions;
                }
                return missingInfoQuestion;
            }
            
            // ✨ 第七步：信息完整，提供智能确认
            String smartConfirmation = generateSmartConfirmation(context);
            context.addClarification("等待用户确认");
            saveOrderContext(sessionId, context); // 更新上下文状态
            
            return smartConfirmation;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 创建订单失败：" + e.getMessage() + 
                "\n\n💡 请尝试更清晰的表达，如：'为张三创建订单，商品苹果10个单价5元'";
        }
    }

    /**
     * 智能检测缺失信息并生成询问
     */
    private String detectMissingInfoAndAsk(OrderContext context) {
        List<String> missingItems = new ArrayList<>();
        List<String> questions = new ArrayList<>();
        
        // 检查客户信息
        if (context.getCustomerName().isEmpty()) {
            missingItems.add("客户信息");
            
            // 根据订单类型生成相应的询问
            if ("PURCHASE".equals(context.getOrderType())) {
                questions.add("🏪 请问是从哪个供应商采购？");
            } else {
                questions.add("👤 请问订单是给哪位客户的？");
            }
        }
        
        // 记录原始输入，用于后续日志诊断
        String originalInput = context.getOriginalInput();
        if (originalInput != null && !originalInput.isEmpty()) {
            System.out.println("🔍 检测缺失信息 - 原始输入: " + originalInput);
        }
        
        // 检查商品信息
        boolean hasMissingPrice = false;
        boolean hasIncompleteProduct = false;
        
        if (context.getProductList().isEmpty()) {
            if (originalInput != null && containsProductInfo(originalInput)) {
                // 尝试再次提取商品信息
                ProductInfo extractedProduct = extractProductFromText(originalInput);
                if (extractedProduct != null) {
                    System.out.println("🔄 从原始输入提取商品: " + extractedProduct.name + 
                                      " x" + extractedProduct.quantity + 
                                      " @" + extractedProduct.unitPrice);
                    context.getProductList().add(extractedProduct);
                }
            } else {
                missingItems.add("商品信息");
                questions.add("📦 请问需要什么商品？（例如：苹果10个单价5元）");
                hasIncompleteProduct = true;
            }
        }
        
        // 如果已经有商品列表，检查商品详细信息
        if (!context.getProductList().isEmpty()) {
            List<String> incompleteProducts = new ArrayList<>();
            
            for (ProductInfo product : context.getProductList()) {
                if (product.name.isEmpty()) {
                    incompleteProducts.add("商品名称");
                    hasIncompleteProduct = true;
                }
                if (product.quantity <= 0) {
                    incompleteProducts.add("商品数量");
                    hasIncompleteProduct = true;
                }
                // 检查价格是否为0或负数
                if (product.unitPrice <= 0) {
                    hasMissingPrice = true;
                }
            }
            
            // 只有当商品信息真正不完整时才询问商品信息
            if (hasIncompleteProduct) {
                missingItems.addAll(incompleteProducts);
                questions.add("📝 商品信息不完整，请补充" + String.join("、", incompleteProducts));
            }
        }
            
        // 单独处理价格缺失情况（只有当商品基本信息完整时才询问价格）
        if (hasMissingPrice && !hasIncompleteProduct) {
            missingItems.add("商品价格");
            
            // 根据订单类型提供不同的价格询问
            if ("PURCHASE".equals(context.getOrderType())) {
                questions.add("💰 请提供商品的采购单价（例如：单价5元/个）");
            } else {
                questions.add("💰 请提供商品的销售单价（例如：单价5元/个）");
            }
        }
        
        // 如果有缺失信息，生成友好的询问回复
        if (!missingItems.isEmpty()) {
            StringBuilder response = new StringBuilder();
            
            // 🧠 根据已有信息智能生成个性化询问
            if (!context.getCustomerName().isEmpty()) {
                response.append("🤝 好的，为").append(context.getCustomerName()).append("创建订单！");
            } else {
                response.append("🤔 好的，我来帮您创建订单！");
            }
            
            if (questions.size() == 1) {
                response.append("还需要一个信息：\n\n");
            } else {
                response.append("还需要补充一些信息：\n\n");
            }
            
            for (int i = 0; i < questions.size(); i++) {
                response.append(questions.get(i));
                if (i < questions.size() - 1) {
                    response.append("\n");
                }
            }
            
            // 🧠 智能示例生成
            response.append("\n\n💡 您可以这样回复：");
            
            // 根据具体情况生成更精准的示例
            if (missingItems.contains("客户信息") && missingItems.contains("商品信息")) {
                response.append("\n'为张三订购苹果10个单价5元'");
            } else if (missingItems.contains("客户信息")) {
                response.append("\n'客户是张三' 或 '给李四'");
                // 如果已有商品信息，提供更具体的示例
                if (!context.getProductList().isEmpty()) {
                    String productName = context.getProductList().get(0).name;
                    if (!productName.isEmpty()) {
                        response.append(" 或 '卖给王五'");
                    }
                }
            } else if (missingItems.contains("商品信息")) {
                response.append("\n'苹果10个单价5元' 或 '香蕉20个每个3元'");
            } else if (missingItems.contains("商品价格")) {
                // 🧠 根据商品名生成具体的价格示例
                if (!context.getProductList().isEmpty()) {
                    String productName = context.getProductList().get(0).name;
                    if (!productName.isEmpty()) {
                        // 生成更具体的价格示例，针对不同类型商品
                        if (productName.contains("书") || productName.contains("教材")) {
                            response.append("\n'").append(productName).append("单价50元' 或 '每本30元'");
                        } else if (productName.contains("电脑") || productName.contains("手机")) {
                            response.append("\n'").append(productName).append("单价5000元' 或 '每台8000元'");
                        } else {
                            response.append("\n'").append(productName).append("单价20元' 或 '每个15元'");
                        }
                    } else {
                        response.append("\n'单价20元' 或 '每个15元'");
                    }
                } else {
                    response.append("\n'单价20元' 或 '每个15元'");
                }
            }
            
            // 🧠 添加智能提示
            String orderTypeHint = "PURCHASE".equals(context.getOrderType()) ? "采购" : "销售";
            if (missingItems.size() == 1 && missingItems.contains("商品价格")) {
                response.append("\n\n💭 这是一个").append(orderTypeHint).append("订单");
            }
            
            return response.toString();
        }
        
        return ""; // 没有缺失信息
    }
    
    /**
     * 检查文本是否包含商品信息
     */
    private boolean containsProductInfo(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // 通用商品单位
        String[] productUnits = {"个", "件", "本", "瓶", "袋", "台", "部", "套", "张", "只", "箱", "斤"};
        
        // 检查是否包含数量单位
        for (String unit : productUnits) {
            if (text.contains(unit)) {
                return true;
            }
        }
        
        // 检查是否包含常见商品关键词
        String[] productKeywords = {"书", "电脑", "手机", "水", "饮料", "苹果", "香蕉", "大米"};
        for (String keyword : productKeywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        
        // 检查是否有"买了X"这样的模式
        if (text.matches(".*买了\\s*\\d+.*") || text.matches(".*买\\s*\\d+.*")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 保存订单上下文
     */
    private void saveOrderContext(String sessionId, OrderContext context) {
        // 如果sessionId为空，生成一个随机ID
        String contextId = sessionId.isEmpty() ? UUID.randomUUID().toString() : sessionId;
        context.setLastUpdateTime(System.currentTimeMillis());
        orderContextCache.put(contextId, context);
        
        // 清理过期上下文
        cleanupExpiredContexts();
    }
    
    /**
     * 获取订单上下文
     */
    private OrderContext getOrderContext(String sessionId) {
        if (sessionId.isEmpty()) {
            // 如果sessionId为空，返回任意一个未完成的上下文（简化处理）
            return orderContextCache.values().stream()
                .findFirst()
                .orElse(null);
        }
        return orderContextCache.get(sessionId);
    }
    
    /**
     * 删除订单上下文
     */
    private void removeOrderContext(String sessionId) {
        if (!sessionId.isEmpty()) {
            orderContextCache.remove(sessionId);
        } else {
            // 如果sessionId为空，清空所有上下文（简化处理）
            orderContextCache.clear();
        }
    }
    
    /**
     * 检查是否有未完成的订单上下文
     */
    private boolean hasIncompleteOrderContext(String sessionId) {
        OrderContext context = getOrderContext(sessionId);
        return context != null;
    }
    
    /**
     * 清理过期上下文
     */
    private void cleanupExpiredContexts() {
        long currentTime = System.currentTimeMillis();
        List<String> expiredKeys = orderContextCache.entrySet().stream()
            .filter(entry -> (currentTime - entry.getValue().getLastUpdateTime()) > CONTEXT_TIMEOUT)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        for (String key : expiredKeys) {
            orderContextCache.remove(key);
        }
    }
    
    /**
     * 订单上下文类 - 存储订单创建过程中的信息
     */
    private static class OrderContext {
        private String orderType;
        private String customerName;
        private List<ProductInfo> productList;
        private long lastUpdateTime;
        private String originalInput; // 保存原始输入，用于智能推理
        private List<String> clarificationHistory; // 澄清历史
        
        public OrderContext(String orderType, String customerName, List<ProductInfo> productList) {
            this.orderType = orderType;
            this.customerName = customerName;
            this.productList = new ArrayList<>(productList);
            this.lastUpdateTime = System.currentTimeMillis();
            this.clarificationHistory = new ArrayList<>();
        }
        
        public String getOrderType() {
            return orderType;
        }
        
        public String getCustomerName() {
            return customerName;
        }
        
        public List<ProductInfo> getProductList() {
            return productList;
        }
        
        public long getLastUpdateTime() {
            return lastUpdateTime;
        }
        
        public void setLastUpdateTime(long lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }
        
        public String getOriginalInput() {
            return originalInput;
        }
        
        public void setOriginalInput(String originalInput) {
            this.originalInput = originalInput;
        }
        
        public List<String> getClarificationHistory() {
            return clarificationHistory;
        }
        
        public void addClarification(String clarification) {
            this.clarificationHistory.add(clarification);
        }
    }
    
    /**
     * 客户偏好类 - 记住客户的购买偏好
     */
    private static class CustomerPreference {
        private List<String> frequentProducts; // 常买商品
        private Map<String, Float> preferredPrices; // 偏好价格
        private String preferredOrderType; // 偏好订单类型
        private long lastOrderTime; // 最后下单时间
        
        public CustomerPreference() {
            this.frequentProducts = new ArrayList<>();
            this.preferredPrices = new HashMap<>();
            this.lastOrderTime = System.currentTimeMillis();
        }
        
        // Getters and setters
        public List<String> getFrequentProducts() { return frequentProducts; }
        public Map<String, Float> getPreferredPrices() { return preferredPrices; }
        public String getPreferredOrderType() { return preferredOrderType; }
        public void setPreferredOrderType(String preferredOrderType) { this.preferredOrderType = preferredOrderType; }
        public long getLastOrderTime() { return lastOrderTime; }
        public void setLastOrderTime(long lastOrderTime) { this.lastOrderTime = lastOrderTime; }
    }
    
    /**
     * 智能推理引擎类 - 提供各种智能推理功能
     */
    private class SmartInferenceEngine {
        
        /**
         * 智能推断缺失信息
         */
        public OrderContext smartInferMissingInfo(OrderContext context, String userInput) {
            // 1. 基于历史偏好推断客户
            if (context.getCustomerName().isEmpty()) {
                String inferredCustomer = inferCustomerFromHistory(userInput);
                if (!inferredCustomer.isEmpty()) {
                    context.customerName = inferredCustomer;
                    context.addClarification("根据历史记录推断客户：" + inferredCustomer);
                }
            }
            // 不再覆盖已有的客户信息
            
            // 2. 基于客户偏好推断商品信息
            if (!context.getCustomerName().isEmpty() && context.getProductList().isEmpty()) {
                List<ProductInfo> inferredProducts = inferProductsFromCustomerHistory(context.getCustomerName(), userInput);
                if (!inferredProducts.isEmpty()) {
                    context.getProductList().addAll(inferredProducts);
                    context.addClarification("基于客户历史推断商品信息");
                }
            }
            
            // 3. 智能推断价格
            for (ProductInfo product : context.getProductList()) {
                if (product.unitPrice <= 0 && !product.name.isEmpty()) {
                    Float inferredPrice = inferPriceFromHistory(product.name, context.getCustomerName());
                    if (inferredPrice != null && inferredPrice > 0) {
                        product.unitPrice = inferredPrice;
                        context.addClarification("使用历史价格：" + product.name + " ¥" + inferredPrice);
                    }
                }
            }
            
            return context;
        }
        
        /**
         * 基于历史记录推断客户
         */
        private String inferCustomerFromHistory(String input) {
            // 检查客户别名映射
            for (Map.Entry<String, String> entry : customerAliasCache.entrySet()) {
                if (input.toLowerCase().contains(entry.getKey().toLowerCase())) {
                    return entry.getValue();
                }
            }
            
            // 模糊匹配已知客户
            return findBestCustomerMatch(input);
        }
        
        /**
         * 基于客户历史推断商品
         */
        private List<ProductInfo> inferProductsFromCustomerHistory(String customerName, String input) {
            List<ProductInfo> inferred = new ArrayList<>();
            CustomerPreference pref = customerPreferenceCache.get(customerName);
            
            if (pref != null && !pref.getFrequentProducts().isEmpty()) {
                // 检查输入中是否提到了客户常买的商品
                for (String product : pref.getFrequentProducts()) {
                    if (containsProduct(input, product)) {
                        Float price = pref.getPreferredPrices().get(product);
                        inferred.add(new ProductInfo(product, 1, price != null ? price : 0));
                    }
                }
            }
            
            return inferred;
        }
        
        /**
         * 基于历史推断价格
         */
        private Float inferPriceFromHistory(String productName, String customerName) {
            // 优先使用客户特定的价格偏好
            CustomerPreference pref = customerPreferenceCache.get(customerName);
            if (pref != null) {
                Float customerPrice = pref.getPreferredPrices().get(productName);
                if (customerPrice != null && customerPrice > 0) {
                    return customerPrice;
                }
            }
            
            // 使用全局商品价格缓存
            return productPriceCache.get(productName);
        }
        
        /**
         * 检查输入是否包含指定商品
         */
        private boolean containsProduct(String input, String product) {
            return input.toLowerCase().contains(product.toLowerCase());
        }
    }
    
    // 创建智能推理引擎实例
    private final SmartInferenceEngine smartEngine = new SmartInferenceEngine();
    
    /**
     * 🧠 智能模糊匹配客户名 - 容错和别名支持
     */
    private String findBestCustomerMatch(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        
        // 收集所有可能的客户名（从已有订单中学习）
        List<String> knownCustomers = new ArrayList<>();
        try {
            // 获取最近的客户名
            Page<Order> recentOrders = orderService.getOrdersByType("SALE", 0, 50);
            knownCustomers = recentOrders.getContent().stream()
                .map(Order::getCustomerName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("获取客户历史失败: " + e.getMessage());
        }
        
        // 添加采购订单的供应商名
        try {
            Page<Order> purchaseOrders = orderService.getOrdersByType("PURCHASE", 0, 50);
            knownCustomers.addAll(purchaseOrders.getContent().stream()
                .map(Order::getCustomerName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList()));
        } catch (Exception e) {
            System.err.println("获取供应商历史失败: " + e.getMessage());
        }
        
        if (knownCustomers.isEmpty()) {
            return "";
        }
        
        // 智能匹配算法
        String bestMatch = "";
        int bestScore = 0;
        
        for (String customer : knownCustomers) {
            int score = calculateMatchScore(input, customer);
            if (score > bestScore && score >= 2) { // 至少要有基本的匹配度
                bestScore = score;
                bestMatch = customer;
            }
        }
        
        return bestMatch;
    }
    
    /**
     * 🧠 计算字符串匹配分数 - 智能相似度算法
     */
    private int calculateMatchScore(String input, String target) {
        if (input == null || target == null) return 0;
        
        String lowerInput = input.toLowerCase();
        String lowerTarget = target.toLowerCase();
        
        int score = 0;
        
        // 1. 完全匹配 - 最高分
        if (lowerInput.contains(lowerTarget) || lowerTarget.contains(lowerInput)) {
            score += 10;
        }
        
        // 2. 首字符匹配
        if (!lowerInput.isEmpty() && !lowerTarget.isEmpty() && 
            lowerInput.charAt(0) == lowerTarget.charAt(0)) {
            score += 3;
        }
        
        // 3. 字符重叠度
        for (char c : lowerTarget.toCharArray()) {
            if (lowerInput.indexOf(c) >= 0) {
                score += 1;
            }
        }
        
        // 4. 长度相似性奖励
        int lengthDiff = Math.abs(input.length() - target.length());
        if (lengthDiff <= 1) {
            score += 2;
        } else if (lengthDiff <= 2) {
            score += 1;
        }
        
        return score;
    }
    
    /**
     * 🧠 学习并更新客户偏好
     */
    private void learnCustomerPreference(String customerName, List<ProductInfo> products, String orderType) {
        if (customerName == null || customerName.trim().isEmpty() || products.isEmpty()) {
            return;
        }
        
        CustomerPreference pref = customerPreferenceCache.computeIfAbsent(customerName, k -> new CustomerPreference());
        
        // 更新常买商品
        for (ProductInfo product : products) {
            if (!product.name.isEmpty()) {
                if (!pref.getFrequentProducts().contains(product.name)) {
                    pref.getFrequentProducts().add(product.name);
                }
                
                // 更新偏好价格（加权平均）
                if (product.unitPrice > 0) {
                    Float currentPrice = pref.getPreferredPrices().get(product.name);
                    if (currentPrice == null) {
                        pref.getPreferredPrices().put(product.name, product.unitPrice);
                        productPriceCache.put(product.name, product.unitPrice); // 同时更新全局缓存
                    } else {
                        // 加权平均：70%历史价格 + 30%新价格
                        float weightedPrice = currentPrice * 0.7f + product.unitPrice * 0.3f;
                        pref.getPreferredPrices().put(product.name, weightedPrice);
                        productPriceCache.put(product.name, weightedPrice);
                    }
                }
            }
        }
        
        // 更新偏好订单类型
        pref.setPreferredOrderType(orderType);
        pref.setLastOrderTime(System.currentTimeMillis());
        
        System.out.println("🧠 学习客户偏好: " + customerName + " 喜欢 " + 
            products.stream().map(p -> p.name).collect(Collectors.joining(", ")));
    }
    
    /**
     * 🧠 智能纠错和别名学习
     */
    private String intelligentErrorCorrection(String input, String fieldType) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        // 常见错别字和简写映射
        Map<String, String> corrections = new HashMap<>();
        
        if ("customer".equals(fieldType)) {
            // 客户名常见错误
            corrections.put("冯天一", "冯天祎");
            corrections.put("张3", "张三");
            corrections.put("李4", "李四");
            corrections.put("老张", "张三");
            corrections.put("小李", "李四");
            corrections.put("小王", "王五");
        } else if ("product".equals(fieldType)) {
            // 商品名常见错误和简写
            corrections.put("苹果🍎", "苹果");
            corrections.put("apple", "苹果");
            corrections.put("water", "水");
            corrections.put("🍎", "苹果");
            corrections.put("🍌", "香蕉");
            corrections.put("🍊", "橙子");
            corrections.put("💧", "水");
            corrections.put("饮用水", "水");
            corrections.put("矿泉水", "水");
        }
        
        // 检查是否需要纠错
        for (Map.Entry<String, String> entry : corrections.entrySet()) {
            if (input.toLowerCase().contains(entry.getKey().toLowerCase())) {
                String corrected = input.replace(entry.getKey(), entry.getValue());
                System.out.println("🔧 智能纠错: " + input + " → " + corrected);
                return corrected;
            }
        }
        
        return input;
    }
    
    /**
     * 🧠 智能生成建议和提示
     */
    private String generateSmartSuggestions(OrderContext context) {
        StringBuilder suggestions = new StringBuilder();
        
        // 基于客户历史生成建议
        if (!context.getCustomerName().isEmpty()) {
            CustomerPreference pref = customerPreferenceCache.get(context.getCustomerName());
            if (pref != null && !pref.getFrequentProducts().isEmpty()) {
                suggestions.append("💡 ").append(context.getCustomerName()).append("常买商品：");
                suggestions.append(pref.getFrequentProducts().stream()
                    .limit(3)  // 只显示前3个
                    .collect(Collectors.joining("、")));
                suggestions.append("\n");
            }
        }
        
        // 基于商品历史生成价格建议
        for (ProductInfo product : context.getProductList()) {
            if (!product.name.isEmpty() && product.unitPrice <= 0) {
                Float suggestedPrice = productPriceCache.get(product.name);
                if (suggestedPrice != null && suggestedPrice > 0) {
                    suggestions.append("💰 ").append(product.name)
                        .append("建议价格：¥").append(String.format("%.2f", suggestedPrice)).append("\n");
                }
            }
        }
        
        return suggestions.toString();
    }
    
    /**
     * 🧠 智能对话式确认
     */
    private String generateSmartConfirmation(OrderContext context) {
        StringBuilder confirmation = new StringBuilder();
        confirmation.append("📋 请确认订单信息：\n\n");
        
        // 订单类型
        String typeDesc = "PURCHASE".equals(context.getOrderType()) ? "采购" : "销售";
        String typeIcon = "PURCHASE".equals(context.getOrderType()) ? "📦" : "💰";
        confirmation.append(typeIcon).append(" 订单类型：").append(typeDesc).append("\n");
        
        // 客户信息
        if (!context.getCustomerName().isEmpty()) {
            String partnerLabel = "PURCHASE".equals(context.getOrderType()) ? "供应商" : "客户";
            confirmation.append("👤 ").append(partnerLabel).append("：").append(context.getCustomerName()).append("\n");
        }
        
        // 商品明细
        if (!context.getProductList().isEmpty()) {
            confirmation.append("📦 商品明细：\n");
            float totalAmount = 0;
            for (ProductInfo product : context.getProductList()) {
                float itemTotal = product.quantity * product.unitPrice;
                totalAmount += itemTotal;
                confirmation.append("  • ").append(product.name)
                    .append(" × ").append(product.quantity)
                    .append(" @ ¥").append(String.format("%.2f", product.unitPrice))
                    .append(" = ¥").append(String.format("%.2f", itemTotal)).append("\n");
            }
            confirmation.append("💵 总金额：¥").append(String.format("%.2f", totalAmount)).append("\n");
        }
        
        // 智能推理历史 - 只显示与客户/供应商无关的推理结果
        if (!context.getClarificationHistory().isEmpty()) {
            List<String> filteredHistory = context.getClarificationHistory().stream()
                .filter(c -> !c.contains("根据历史记录推断客户"))
                .collect(Collectors.toList());
                
            if (!filteredHistory.isEmpty()) {
                confirmation.append("\n🤖 AI推理：\n");
                for (String clarification : filteredHistory) {
                    confirmation.append("  • ").append(clarification).append("\n");
                }
            }
        }
        
        confirmation.append("\n💬 确认创建请回复：'是'\n");
        confirmation.append("💬 需要修改请直接说明：'客户改为XX' 或 '价格改为XX元'\n");
        
        return confirmation.toString();
    }

    /**
     * 删除订单
     */
    private String handleDeleteOrder(JsonNode root) {
        try {
            long orderId = getLongValue(root, "order_id", "id", "订单ID");
            
            if (orderId <= 0) {
                return "❌ 请提供有效的订单ID\n💡 示例：'删除订单123' 或 '删除ID为123的订单'";
            }

            // 直接删除订单，依靠 OrderService 的异常处理
            try {
                orderService.deleteOrder(orderId);
                return "✅ 订单删除成功\n\n🗑️ 已删除订单ID：" + orderId;
                
            } catch (Exception e) {
                if (e.getMessage().contains("not found") || e.getMessage().contains("不存在") || 
                    e.getMessage().contains("No value present")) {
                    return "❌ 找不到ID为 " + orderId + " 的订单\n💡 请检查订单ID是否正确";
                }
                throw e;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 删除订单失败：" + e.getMessage();
        }
    }

    /**
     * 查询订单 - 简化版本，提供基础信息
     */
    private String handleQueryOrder(JsonNode root) {
        try {
            String keyword = getStringValue(root, "keyword", "search", "关键词");
            String orderType = getStringValue(root, "order_type", "type", "订单类型");
            int limit = getIntValue(root, "limit", "count", "数量");
            if (limit <= 0) limit = 10; // 默认返回10条

            // 获取订单
            List<Order> allOrders = new ArrayList<>();
            
            if (orderType.isEmpty() || orderType.equalsIgnoreCase("SALE")) {
                Page<Order> salesOrders = orderService.getOrdersByType("SALE", 0, limit);
                allOrders.addAll(salesOrders.getContent());
            }
            
            if (orderType.isEmpty() || orderType.equalsIgnoreCase("PURCHASE")) {
                Page<Order> purchaseOrders = orderService.getOrdersByType("PURCHASE", 0, limit);
                allOrders.addAll(purchaseOrders.getContent());
            }

            // 关键词筛选
            if (!keyword.isEmpty()) {
                allOrders = allOrders.stream()
                    .filter(order -> matchesKeyword(order, keyword))
                    .collect(Collectors.toList());
            }

            // 限制数量
            if (allOrders.size() > limit) {
                allOrders = allOrders.subList(0, limit);
            }

            if (allOrders.isEmpty()) {
                String searchInfo = keyword.isEmpty() ? "" : "关键词'" + keyword + "'";
                return "📭 没有找到相关订单" + (searchInfo.isEmpty() ? "" : "（" + searchInfo + "）") + 
                    "\n\n💡 试试：\n• 查询所有订单\n• 查询销售订单\n• 查询客户张三的订单";
            }

            // 生成简洁的订单列表
            StringBuilder result = new StringBuilder();
            result.append("🔍 查询到 ").append(allOrders.size()).append(" 个订单：\n\n");

            for (int i = 0; i < Math.min(allOrders.size(), 5); i++) { // 最多显示5个
                Order order = allOrders.get(i);
                String typeIcon = order.getOrderType().equals("SALE") ? "💰" : "📦";
                String statusIcon = getStatusIcon(order.getStatus());
                
                result.append(typeIcon).append(" ").append(order.getOrderNo())
                    .append(" | ").append(order.getCustomerName())
                    .append(" | ¥").append(String.format("%.2f", order.getAmount()))
                    .append(" ").append(statusIcon).append("\n");
            }

            if (allOrders.size() > 5) {
                result.append("\n... 还有 ").append(allOrders.size() - 5).append(" 个订单\n");
            }

            result.append("\n💡 如需详细分析，请说：'分析这些订单'");
            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 查询订单失败：" + e.getMessage();
        }
    }

    /**
     * 确认订单
     */
    private String handleConfirmOrder(JsonNode root) {
        try {
            long orderId = getLongValue(root, "order_id", "id", "订单ID");
            float freight = getFloatValue(root, "freight", "shipping", "运费");

            if (orderId <= 0) {
                return "❌ 请提供有效的订单ID\n💡 示例：'确认订单123，运费10元'";
            }

            if (freight < 0) {
                return "❌ 运费不能为负数\n💡 如无运费请设为0";
            }

            Order confirmedOrder = orderService.confirmOrder(orderId, freight);
            
            return String.format("✅ 订单确认成功！\n\n📋 确认详情：\n• 订单号：%s\n• 客户：%s\n• 订单金额：¥%.2f\n• 运费：¥%.2f\n• 总计：¥%.2f", 
                confirmedOrder.getOrderNo(), confirmedOrder.getCustomerName(), 
                confirmedOrder.getAmount(), freight, confirmedOrder.getAmount() + freight);

        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage().contains("not found") || e.getMessage().contains("不存在")) {
                return "❌ 找不到指定的订单\n💡 请检查订单ID是否正确";
            }
            return "❌ 确认订单失败：" + e.getMessage();
        }
    }

    /**
     * 销售查询
     */
    private String handleQuerySales(JsonNode root) {
        try {
            String timeRange = getStringValue(root, "time_range", "period", "时间范围");
            String customer = getStringValue(root, "customer", "client", "客户");
            
            Page<Order> salesOrders = orderService.getOrdersByType("SALE", 0, 50);
            List<Order> orders = salesOrders.getContent();

            if (!customer.isEmpty()) {
                orders = orders.stream()
                    .filter(order -> order.getCustomerName() != null && 
                            order.getCustomerName().contains(customer))
                    .collect(Collectors.toList());
            }

            if (orders.isEmpty()) {
                return "📊 暂无销售数据" + (customer.isEmpty() ? "" : "（客户：" + customer + "）");
            }

            double totalAmount = orders.stream().mapToDouble(Order::getAmount).sum();
            int totalOrders = orders.size();
            double avgAmount = totalAmount / totalOrders;

            StringBuilder result = new StringBuilder();
            result.append("💰 销售数据统计").append(timeRange.isEmpty() ? "" : "（" + timeRange + "）").append("：\n\n");
            result.append("📈 总销售额：¥").append(String.format("%.2f", totalAmount)).append("\n");
            result.append("📋 订单数量：").append(totalOrders).append("个\n");
            result.append("📊 平均订单金额：¥").append(String.format("%.2f", avgAmount)).append("\n");

            if (!customer.isEmpty()) {
                result.append("👤 客户：").append(customer).append("\n");
            }

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 销售查询失败：" + e.getMessage();
        }
    }

    /**
     * 库存查询（暂时返回提示信息）
     */
    private String handleQueryInventory(JsonNode root) {
        return "📦 库存查询功能开发中...\n\n💡 您可以尝试：\n• 查询订单\n• 查询销售数据\n• 创建新订单";
    }

    /**
     * 财务分析（暂时返回提示信息）
     */
    private String handleAnalyzeFinance(JsonNode root) {
        return "📊 财务分析功能开发中...\n\n💡 您可以尝试：\n• 查询销售数据\n• 查询订单信息";
    }

    /**
     * 订单数据分析 - 智能订单洞察
     */
    private String handleAnalyzeOrder(JsonNode root) {
        try {
            // 获取筛选参数
            String orderType = getStringValue(root, "order_type", "type", "订单类型");
            String customer = getStringValue(root, "customer", "client", "客户");
            int limit = getIntValue(root, "limit", "count", "数量");
            if (limit <= 0) limit = 100; // 分析更多数据

            // 获取所有相关订单数据 - 优化查询性能
            List<Order> allOrders = new ArrayList<>();
            
            System.out.println("🔍 开始查询订单数据，类型: " + orderType + ", 限制: " + limit);
            
            try {
                if (orderType.isEmpty() || orderType.equalsIgnoreCase("SALE")) {
                    Page<Order> salesOrders = orderService.getOrdersByType("SALE", 0, limit);
                    allOrders.addAll(salesOrders.getContent());
                    System.out.println("✅ 销售订单查询完成: " + salesOrders.getContent().size() + "条");
                }
                
                if (orderType.isEmpty() || orderType.equalsIgnoreCase("PURCHASE")) {
                    Page<Order> purchaseOrders = orderService.getOrdersByType("PURCHASE", 0, limit);
                    allOrders.addAll(purchaseOrders.getContent());
                    System.out.println("✅ 采购订单查询完成: " + purchaseOrders.getContent().size() + "条");
                }
            } catch (Exception dbError) {
                System.err.println("❌ 数据库查询失败: " + dbError.getMessage());
                return "❌ 数据查询失败：" + dbError.getMessage() + "\n\n💡 请稍后重试或检查数据库连接";
            }

            // 客户筛选
            if (!customer.isEmpty()) {
                allOrders = allOrders.stream()
                    .filter(order -> order.getCustomerName() != null && 
                            order.getCustomerName().contains(customer))
                    .collect(Collectors.toList());
            }

            if (allOrders.isEmpty()) {
                return "📭 没有找到订单数据进行分析\n\n💡 请先创建一些订单，或调整筛选条件";
            }

            // 构建AI分析请求
            StringBuilder analysisData = new StringBuilder();
            analysisData.append("📊 订单数据分析请求 (共").append(allOrders.size()).append("个订单)\n\n");
            
            // 基础统计数据
            List<Order> salesOrders = allOrders.stream()
                .filter(o -> "SALE".equals(o.getOrderType()))
                .collect(Collectors.toList());
            List<Order> purchaseOrders = allOrders.stream()
                .filter(o -> "PURCHASE".equals(o.getOrderType()))
                .collect(Collectors.toList());

            analysisData.append("📈 销售订单: ").append(salesOrders.size()).append("个\n");
            analysisData.append("📦 采购订单: ").append(purchaseOrders.size()).append("个\n\n");

            // 状态分布
            Map<String, Long> statusStats = allOrders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
            analysisData.append("📋 订单状态分布:\n");
            statusStats.forEach((status, count) -> 
                analysisData.append("  • ").append(status).append(": ").append(count).append("个\n"));

            // 金额统计
            double totalSalesAmount = salesOrders.stream().mapToDouble(Order::getAmount).sum();
            double totalPurchaseAmount = purchaseOrders.stream().mapToDouble(Order::getAmount).sum();
            
            analysisData.append("\n💰 金额统计:\n");
            analysisData.append("  • 销售总额: ¥").append(String.format("%.2f", totalSalesAmount)).append("\n");
            analysisData.append("  • 采购总额: ¥").append(String.format("%.2f", totalPurchaseAmount)).append("\n");
            analysisData.append("  • 毛利润: ¥").append(String.format("%.2f", totalSalesAmount - totalPurchaseAmount)).append("\n");

            // 客户分析
            Map<String, Long> customerStats = allOrders.stream()
                .filter(o -> o.getCustomerName() != null && !o.getCustomerName().trim().isEmpty())
                .collect(Collectors.groupingBy(Order::getCustomerName, Collectors.counting()));
            
            if (!customerStats.isEmpty()) {
                analysisData.append("\n👥 客户订单分布 (TOP 5):\n");
                customerStats.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> 
                        analysisData.append("  • ").append(entry.getKey()).append(": ").append(entry.getValue()).append("个订单\n"));
            }

            // 平均订单金额
            if (!allOrders.isEmpty()) {
                double avgAmount = allOrders.stream().mapToDouble(Order::getAmount).average().orElse(0);
                analysisData.append("\n📊 平均订单金额: ¥").append(String.format("%.2f", avgAmount)).append("\n");
            }

            // 时间分析（最近订单）
            List<Order> recentOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null)
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());

            if (!recentOrders.isEmpty()) {
                analysisData.append("\n🕒 最近订单趋势:\n");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
                for (Order order : recentOrders) {
                    String typeIcon = "SALE".equals(order.getOrderType()) ? "💰" : "📦";
                    analysisData.append("  ").append(typeIcon).append(" ")
                        .append(order.getCreatedAt().format(formatter)).append(" | ")
                        .append(order.getCustomerName() != null ? order.getCustomerName() : "未知客户").append(" | ¥")
                        .append(String.format("%.2f", order.getAmount())).append("\n");
                }
            }

            // 调用AI进行深度分析 - 优化超时处理
            try {
                System.out.println("🤖 开始AI订单分析，数据长度: " + analysisData.length());
                
                // 🆕 明确标记这是分析结果，而非确认流程
                StringBuilder result = new StringBuilder();
                result.append("📊 订单分析\n\n");
                
                // 尝试快速AI分析
                String aiAnalysis = deepSeekAIService.analyzeOrderData(analysisData.toString());
                
                // 清理AI输出中的markdown格式
                String cleanedAnalysis = cleanMarkdownFormat(aiAnalysis);
                
                result.append(cleanedAnalysis);
                
                return result.toString();
                
            } catch (Exception aiError) {
                // AI调用失败时，返回增强版基础统计分析
                System.err.println("⚠️ AI分析超时/失败，使用本地分析: " + aiError.getMessage());
                
                // 🆕 明确标记这是分析结果，而非确认流程
                StringBuilder result = new StringBuilder();
                result.append("📊 快速订单分析（本地处理）\n\n");
                
                result.append(generateLocalOrderAnalysis(allOrders, salesOrders, purchaseOrders, 
                    totalSalesAmount, totalPurchaseAmount, customerStats, analysisData.toString()));
                
                return result.toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 订单分析失败：" + e.getMessage() + "\n\n💡 请稍后重试或联系管理员";
        }
    }

    /**
     * 生成本地订单分析报告 - AI分析失败时的fallback
     */
    private String generateLocalOrderAnalysis(List<Order> allOrders, List<Order> salesOrders, 
                                            List<Order> purchaseOrders, double totalSalesAmount, 
                                            double totalPurchaseAmount, Map<String, Long> customerStats, 
                                            String basicData) {
        StringBuilder result = new StringBuilder();
        result.append("📊 快速订单分析报告 (本地分析)\n\n");
        
        // 核心指标总结
        result.append("🎯 核心指标\n");
        result.append("• 订单总数：").append(allOrders.size()).append("个\n");
        result.append("• 销售订单：").append(salesOrders.size()).append("个 | 采购订单：").append(purchaseOrders.size()).append("个\n");
        result.append("• 销售总额：¥").append(String.format("%.2f", totalSalesAmount)).append("\n");
        result.append("• 采购总额：¥").append(String.format("%.2f", totalPurchaseAmount)).append("\n");
        result.append("• 毛利润：¥").append(String.format("%.2f", totalSalesAmount - totalPurchaseAmount)).append("\n\n");
        
        // 智能洞察
        result.append("💡 业务洞察\n");
        
        // 业务结构分析
        if (salesOrders.size() > purchaseOrders.size() * 2) {
            result.append("• 🔥 销售主导型业务，销售活跃度高，建议加强库存管理\n");
        } else if (purchaseOrders.size() > salesOrders.size() * 2) {
            result.append("• 📦 采购密集期，可能在备货或业务扩张，关注资金流动\n");
        } else {
            result.append("• ⚖️ 销采平衡，业务运营相对稳定\n");
        }
        
        // 盈利分析
        if (totalSalesAmount > totalPurchaseAmount) {
            double profitMargin = ((totalSalesAmount - totalPurchaseAmount) / totalSalesAmount) * 100;
            if (profitMargin > 50) {
                result.append("• 💚 盈利优秀，毛利率达 ").append(String.format("%.1f%%", profitMargin)).append("，业务健康\n");
            } else if (profitMargin > 20) {
                result.append("• 💙 盈利良好，毛利率约 ").append(String.format("%.1f%%", profitMargin)).append("，可持续发展\n");
            } else {
                result.append("• 💛 盈利偏低，毛利率仅 ").append(String.format("%.1f%%", profitMargin)).append("，需优化成本\n");
            }
        } else {
            result.append("• ⚠️ 成本压力，支出超过收入，需重点关注现金流\n");
        }
        
        // 客户结构分析
        if (!customerStats.isEmpty()) {
            String topCustomer = customerStats.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("未知");
            long topCount = customerStats.values().stream().max(Long::compareTo).orElse(0L);
            
            if (customerStats.size() == 1) {
                result.append("• 👤 单一客户依赖，主要客户：").append(topCustomer).append("，建议拓展客户群\n");
            } else if (topCount > allOrders.size() * 0.5) {
                result.append("• 👑 头部客户集中，").append(topCustomer).append(" 贡献超过50%订单，注意客户风险\n");
            } else {
                result.append("• 👥 客户分布良好，前5客户较为均衡，业务风险分散\n");
            }
        }
        
        // 平均订单分析
        if (!allOrders.isEmpty()) {
            double avgAmount = (totalSalesAmount + totalPurchaseAmount) / allOrders.size();
            if (avgAmount > 1000) {
                result.append("• 💎 高价值订单，平均金额 ¥").append(String.format("%.0f", avgAmount)).append("，客户质量较高\n");
            } else if (avgAmount > 100) {
                result.append("• 💼 中等订单规模，平均金额 ¥").append(String.format("%.0f", avgAmount)).append("，业务稳健\n");
            } else {
                result.append("• 🛒 小额订单为主，平均金额 ¥").append(String.format("%.0f", avgAmount)).append("，可考虑提升客单价\n");
            }
        }
        
        result.append("\n🚀 优化建议\n");
        
        // 基于数据的具体建议
        if (totalSalesAmount > totalPurchaseAmount * 3) {
            result.append("• 增加采购频次，避免库存断货影响销售\n");
        }
        if (customerStats.size() <= 3 && allOrders.size() > 10) {
            result.append("• 拓展客户群体，降低客户集中风险\n");
        }
        if (!allOrders.isEmpty()) {
            long pendingCount = allOrders.stream()
                .filter(o -> "PENDING".equals(o.getStatus()))
                .count();
            if (pendingCount > allOrders.size() * 0.3) {
                result.append("• 及时处理待确认订单，提升客户满意度\n");
            }
        }
        
        result.append("• 定期分析订单趋势，制定数据驱动的业务策略\n");
        result.append("• 关注现金流，优化收付款周期\n");
        
        return result.toString();
    }

    /**
     * 清理AI输出中的markdown格式
     */
    private String cleanMarkdownFormat(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        // 移除markdown粗体标记
        String cleaned = text.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        
        // 移除其他markdown标记
        cleaned = cleaned.replaceAll("\\*([^*]+)\\*", "$1");  // 斜体
        cleaned = cleaned.replaceAll("```[\\s\\S]*?```", "");  // 代码块
        cleaned = cleaned.replaceAll("`([^`]+)`", "$1");      // 行内代码
        
        return cleaned.trim();
    }

    // 辅助方法：获取字符串值（支持多个字段名）
    private String getStringValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName) && !node.get(fieldName).asText().isEmpty()) {
                return node.get(fieldName).asText().trim();
            }
        }
        return "";
    }

    // 辅助方法：获取整数值
    private int getIntValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName)) {
                return node.get(fieldName).asInt(0);
            }
        }
        return 0;
    }

    // 辅助方法：获取长整数值
    private long getLongValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName)) {
                return node.get(fieldName).asLong(0L);
            }
        }
        return 0L;
    }

    // 辅助方法：获取浮点数值
    private float getFloatValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName)) {
                return (float) node.get(fieldName).asDouble(0.0);
            }
        }
        return 0.0f;
    }

    // 辅助方法：获取数组值
    private JsonNode getArrayValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName) && node.get(fieldName).isArray()) {
                return node.get(fieldName);
            }
        }
        return mapper.createArrayNode(); // 返回空数组
    }

    // 辅助方法：检查订单是否匹配关键词
    private boolean matchesKeyword(Order order, String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        
        return (order.getCustomerName() != null && order.getCustomerName().toLowerCase().contains(lowerKeyword)) ||
               (order.getOrderNo() != null && order.getOrderNo().toLowerCase().contains(lowerKeyword)) ||
               (order.getGoods() != null && order.getGoods().stream().anyMatch(og -> 
                   og.getGoods() != null && og.getGoods().getName() != null && 
                   og.getGoods().getName().toLowerCase().contains(lowerKeyword)));
    }

    // 辅助方法：获取状态图标
    private String getStatusIcon(Object status) {
        if (status == null) return "⏳";
        String statusStr = status.toString().toLowerCase();
        return switch (statusStr) {
            case "confirmed", "完成" -> "✅";
            case "pending", "待确认" -> "⏳";
            case "cancelled", "已取消" -> "❌";
            default -> "📝";
        };
    }

    /**
     * 产品信息内部类
     */
    private static class ProductInfo {
        String name;
        int quantity;
        float unitPrice;
        
        ProductInfo(String name, int quantity, float unitPrice) {
            this.name = name;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }
    }

    /**
     * 判断输入是否仅包含价格信息
     */
    private boolean isPriceOnlyInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        // 价格专用模式
        String[] priceOnlyPatterns = {
            "^\\s*单价\\s*\\d+(?:\\.\\d+)?\\s*元?\\s*$",                  // 单价5元
            "^\\s*价格\\s*\\d+(?:\\.\\d+)?\\s*元?\\s*$",                  // 价格5元
            "^\\s*\\d+(?:\\.\\d+)?\\s*元/?(?:个|瓶|件|只|袋|箱|斤)\\s*$",  // 5元/个
            "^\\s*\\d+(?:\\.\\d+)?\\s*[块钱]/?(?:个|瓶|件|只|袋|箱|斤)?\\s*$", // 5块一个
            "^\\s*每\\s*(?:个|瓶|件|只|袋|箱|斤)\\s*\\d+(?:\\.\\d+)?\\s*元?\\s*$", // 每个5元
            "^\\s*一\\s*(?:个|瓶|件|只|袋|箱|斤)\\s*\\d+(?:\\.\\d+)?\\s*元?\\s*$", // 一个5元
            "^\\s*[\\u4e00-\\u9fa5]*单价\\s*\\d+(?:\\.\\d+)?\\s*元?\\s*$",  // 水单价5元
        };
        
        for (String pattern : priceOnlyPatterns) {
            if (input.matches(pattern)) {
                return true;
            }
        }
        
        // 更宽松的判断：短文本且包含价格关键词和数字
        if (input.length() < 20 && 
            (input.contains("元") || input.contains("块") || input.contains("钱") || 
             input.contains("单价") || input.contains("价格") || input.contains("每个") ||
             input.contains("一瓶") || input.contains("一个"))) {
            
            // 确保有数字
            return input.matches(".*\\d+.*");
        }
        
        return false;
    }
    
    /**
     * 从仅包含价格信息的输入中提取价格
     */
    private float extractPriceOnly(String input) {
        if (input == null || input.trim().isEmpty()) {
            return 0;
        }
        
        String[] simplePricePatterns = {
            "(\\d+(?:\\.\\d+)?)\\s*元",                           // 5元
            "(\\d+(?:\\.\\d+)?)\\s*块",                           // 5块
            "(\\d+(?:\\.\\d+)?)\\s*钱",                           // 5钱
            "单价\\s*(\\d+(?:\\.\\d+)?)",                         // 单价5
            "价格\\s*(\\d+(?:\\.\\d+)?)",                         // 价格5
            "[\\u4e00-\\u9fa5]*单价\\s*(\\d+(?:\\.\\d+)?)",       // 水单价5
            "(\\d+(?:\\.\\d+)?)\\s*元/?(?:个|瓶|件|只|袋|箱|斤)",   // 5元/个
            "每\\s*(?:个|瓶|件|只|袋|箱|斤)\\s*(\\d+(?:\\.\\d+)?)", // 每个5
            "一\\s*(?:个|瓶|件|只|袋|箱|斤)\\s*(\\d+(?:\\.\\d+)?)", // 一个5
        };
        
        for (String pattern : simplePricePatterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(input);
            if (m.find()) {
                try {
                    return Float.parseFloat(m.group(1));
                } catch (NumberFormatException e) {
                    // 继续尝试下一个模式
                }
            }
        }
        
        // 兜底方案：尝试提取任何数字
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)");
        java.util.regex.Matcher m = p.matcher(input);
        if (m.find()) {
            try {
                return Float.parseFloat(m.group(1));
            } catch (NumberFormatException e) {
                // 忽略并返回0
            }
        }
        
        return 0;
    }

    /**
     * 智能提取订单类型 - 增强版
     */
    private String smartExtractOrderType(JsonNode root) {
        // 1. 先进行本地强制检查 - 🆕 新增优先检查
        if (root.has("original_input")) {
            String input = root.get("original_input").asText().toLowerCase();
            String localDetection = detectOrderTypeFromText(input);
            if (localDetection.equals("PURCHASE")) {
                System.out.println("🔴 本地强制纠正：检测到采购模式，忽略AI结果: " + input);
                return "PURCHASE";
            }
        }
        
        // 2. 尝试从JSON字段中提取（但会被上面的本地检查覆盖）
        String[] typeFields = {"order_type", "type", "orderType", "order_type"};
        for (String field : typeFields) {
            if (root.has(field)) {
                String type = root.get(field).asText().toUpperCase();
                if (type.equals("SALE") || type.equals("PURCHASE")) {
                    System.out.println("📦 从字段提取订单类型: " + type);
                    
                    // 🆕 双重验证：如果AI说是销售但本地检测是采购，强制纠正
                    if (type.equals("SALE") && root.has("original_input")) {
                        String input = root.get("original_input").asText().toLowerCase();
                        String localType = detectOrderTypeFromText(input);
                        if (localType.equals("PURCHASE")) {
                            System.out.println("🔴 强制纠正AI错误：" + input + " 应该是采购订单，不是销售订单！");
                            return "PURCHASE";
                        }
                    }
                    
                    return type;
                }
            }
        }
        
        // 3. 从原始输入中基于关键词识别
        if (root.has("original_input")) {
            String input = root.get("original_input").asText().toLowerCase();
            String detectedType = detectOrderTypeFromText(input);
            if (!detectedType.isEmpty()) {
                System.out.println("📦 从文本识别订单类型: " + detectedType);
                return detectedType;
            }
        }
        
        // 4. 尝试从其他字段推断
        String allText = root.toString().toLowerCase();
        String inferredType = detectOrderTypeFromText(allText);
        if (!inferredType.isEmpty()) {
            System.out.println("📦 从JSON推断订单类型: " + inferredType);
            return inferredType;
        }
        
        // 5. 默认为销售订单
        System.out.println("📦 使用默认订单类型: SALE");
        return "SALE";
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
            "批发", "进购", "采买", "购进", "收货", "进材料", "买材料"
        };
        
        // 🆕 特殊正则模式检查 - 处理"从XX买"这种表达
        String[] purchasePatterns = {
            "从.*买", "从.*购买", "从.*采购", "从.*进货", "从.*那里", "从.*这里", "从.*处",
            "向.*买", "向.*购买", "向.*采购", "向.*进货"
        };
        
        // 先检查正则模式
        for (String pattern : purchasePatterns) {
            if (text.matches(".*" + pattern + ".*")) {
                System.out.println("🛒 检测到采购模式: " + pattern + " 在文本: " + text);
                return "PURCHASE";
            }
        }
        
        // 再检查普通关键词
        for (String keyword : purchaseKeywords) {
            if (text.contains(keyword)) {
                System.out.println("🛒 检测到采购关键词: " + keyword);
                return "PURCHASE";
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

    /**
     * 智能提取客户信息
     */
    private String smartExtractCustomer(JsonNode root) {
        // 尝试多种字段名和格式
        String[] customerFields = {"customer", "customer_name", "customerName", "client", "supplier", "供应商", "客户"};
        
        for (String field : customerFields) {
            if (root.has(field) && !root.get(field).asText().trim().isEmpty()) {
                return root.get(field).asText().trim();
            }
        }
        
        // 尝试从原始指令中提取（如果有的话）
        if (root.has("original_input")) {
            String input = root.get("original_input").asText();
            // 使用正则表达式匹配常见模式
            return extractCustomerFromText(input);
        }
        
        return "";
    }

    /**
     * 从文本中提取客户名称 - 增强版
     */
    private String extractCustomerFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        // 更全面的客户表达模式 - 优化匹配顺序，先尝试采购模式
        String[] patterns = {
            // 🆕 优先检查：从XX处/那里购买的模式 (采购订单)
            "从\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*那里",     // 从哈振宇那里
            "从\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*这里",     // 从张三这里
            "从\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*处",       // 从李四处
            "从\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*买",       // 从王五买
            "从\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*购买",     // 从张三购买
            "从\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*采购",     // 从供应商采购
            "从\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*进",       // 从供应商进
            "向\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*买",       // 向厂家买
            "向\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*购买",     // 向供应商购买
            
            // 销售给XX的模式  
            "卖给了?\\s*([\\u4e00-\\u9fa5a-zA-Z]+?)(?:\\s|$|[\\d一二三四五六七八九十])",       // 卖给张三 / 卖给了张三（非贪婪匹配）
            "售给\\s*([\\u4e00-\\u9fa5a-zA-Z]+?)(?:\\s|$|[\\d一二三四五六七八九十])",           // 售给李四
            "发给\\s*([\\u4e00-\\u9fa5a-zA-Z]+?)(?:\\s|$|[\\d一二三四五六七八九十])",           // 发给王五
            "交付给\\s*([\\u4e00-\\u9fa5a-zA-Z]+?)(?:\\s|$|[\\d一二三四五六七八九十])",         // 交付给客户
            "出售给\\s*([\\u4e00-\\u9fa5a-zA-Z]+?)(?:\\s|$|[\\d一二三四五六七八九十])",         // 出售给张三
            "卖了.*给\\s*([\\u4e00-\\u9fa5a-zA-Z]+?)(?:\\s|$|[\\d一二三四五六七八九十])",       // 卖了XX给张三
            
            // 基础创建模式
            "为\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*创建",     // 为张三创建
            "给\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*创建",     // 给张三创建 
            "帮\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*创建",     // 帮张三创建
            "为\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*下",       // 为张三下单
            "给\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*下",       // 给张三下单
            "帮\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*买",       // 帮张三买
            
            // 标准格式
            "客户[:：]?\\s*([\\u4e00-\\u9fa5a-zA-Z]+)",      // 客户：张三
            "供应商[:：]?\\s*([\\u4e00-\\u9fa5a-zA-Z]+)",    // 供应商：张三
            "([\\u4e00-\\u9fa5a-zA-Z]+)\\s*的订单",          // 张三的订单
            "([\\u4e00-\\u9fa5a-zA-Z]+)\\s*要",             // 张三要
            "([\\u4e00-\\u9fa5a-zA-Z]+)\\s*订购",           // 张三订购
            
            // 灵活的中文表达模式
            "([\\u4e00-\\u9fa5a-zA-Z]+)\\s*说",             // 张三说
            "([\\u4e00-\\u9fa5a-zA-Z]+)\\s*需要",           // 李四需要  
            "([\\u4e00-\\u9fa5a-zA-Z]+)\\s*想要",           // 王五想要
            "和\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*",         // 和张三
            "跟\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*"          // 跟李四
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) {
                String customerName = m.group(1).trim();
                // 过滤掉一些明显不是客户名的词 - 扩展过滤词汇
                if (!isInvalidCustomerName(customerName)) {
                    System.out.println("🎯 从文本中提取到客户: " + customerName);
                    return customerName;
                }
            }
        }
        
        return "";
    }
    
    /**
     * 🆕 判断是否为无效的客户名
     */
    private boolean isInvalidCustomerName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return true;
        }
        
        // 扩展的无效客户名词汇列表
        String[] invalidNames = {
            // 操作词汇
            "创建", "订单", "下单", "购买", "买", "卖", "销售", "查询", "删除",
            // 商品词汇
            "商品", "苹果", "橙子", "香蕉", "梨子", "葡萄", "西瓜", "草莓", "芒果", "桃子", "樱桃",
            "大米", "面粉", "面条", "馒头", "包子", "饺子", "汤圆", "水", "饮料", "牛奶",
            "鸡蛋", "鱼", "肉", "鸡", "鸭", "猪肉", "牛肉", "羊肉",
            "青菜", "白菜", "萝卜", "土豆", "西红柿", "黄瓜", "茄子",
            // 数量单位词汇
            "数量", "单价", "价格", "元", "块", "钱", "个", "件", "只", "瓶", "袋", "箱", "斤", "公斤",
            // 数量+单位组合
            "一瓶", "一个", "一件", "一只", "一袋", "一箱", "一斤", "三瓶", "五个", "十件",
            // 其他系统词汇
            "订单", "客户", "供应商", "那里", "这里", "地方", "处"
        };
        
        // 特殊情况：如果名称是"hzy"或者其他明显的客户名，直接允许
        // 这样可以确保正确识别特定客户名
        if (name.equalsIgnoreCase("hzy")) {
            return false;
        }
        
        String lowerName = name.toLowerCase();
        for (String invalid : invalidNames) {
            if (lowerName.equals(invalid) || lowerName.equals(invalid.toLowerCase())) {
                return true;
            }
        }
        
        // 检查是否只包含数字（可能是误识别的数量）
        if (name.matches("^\\d+$")) {
            return true;
        }
        
        return false;
    }

    /**
     * 智能提取商品列表 - 增强版
     */
    private List<ProductInfo> smartExtractProducts(JsonNode root) {
        List<ProductInfo> products = new ArrayList<>();
        
        // 尝试从products数组提取
        String[] productArrayFields = {"products", "goods", "items", "商品", "货物"};
        for (String field : productArrayFields) {
            if (root.has(field) && root.get(field).isArray()) {
                JsonNode array = root.get(field);
                for (JsonNode item : array) {
                    ProductInfo product = extractProductFromNode(item);
                    if (product != null) {
                        System.out.println("🛒 从数组提取商品: " + product.name + " x" + product.quantity + " @" + product.unitPrice);
                        products.add(product);
                    }
                }
                break;
            }
        }
        
        // 如果没有找到数组，尝试单个产品字段
        if (products.isEmpty()) {
            ProductInfo singleProduct = extractSingleProduct(root);
            if (singleProduct != null) {
                System.out.println("🛒 提取单个商品: " + singleProduct.name + " x" + singleProduct.quantity + " @" + singleProduct.unitPrice);
                products.add(singleProduct);
            }
        }
        
        // 如果还是没有商品，尝试从原始输入中用正则表达式提取
        if (products.isEmpty() && root.has("original_input")) {
            String input = root.get("original_input").asText();
            ProductInfo extractedProduct = extractProductFromText(input);
            if (extractedProduct != null) {
                System.out.println("🛒 从文本提取商品: " + extractedProduct.name + " x" + extractedProduct.quantity + " @" + extractedProduct.unitPrice);
                products.add(extractedProduct);
            }
        }
        
        // 检查是否是价格补充信息
        if (products.isEmpty() && root.has("original_input")) {
            String input = root.get("original_input").asText().trim();
            
            // 检测是否是单纯的价格信息
            if (isPriceOnlyInput(input)) {
                float price = extractPriceOnly(input);
                if (price > 0) {
                    // 尝试从上下文中提取商品信息
                    // 这里简化处理，创建一个带有价格但无具体商品信息的对象
                    ProductInfo priceInfo = new ProductInfo("", 0, price);
                    System.out.println("💰 提取到价格补充信息: " + price);
                    products.add(priceInfo);
                }
            }
        }
        
        return products;
    }

    /**
     * 从单个节点提取产品信息
     */
    private ProductInfo extractProductFromNode(JsonNode node) {
        String name = getStringValue(node, "name", "product", "productName", "商品名", "产品名");
        int quantity = getIntValue(node, "quantity", "qty", "count", "数量", "个数");
        float unitPrice = getFloatValue(node, "unit_price", "price", "unitPrice", "单价", "价格");
        
        if (!name.isEmpty() && quantity > 0) {
            return new ProductInfo(name, quantity, Math.max(0, unitPrice));
        }
        
        return null;
    }

    /**
     * 提取单个产品信息（当没有数组时）
     */
    private ProductInfo extractSingleProduct(JsonNode root) {
        String name = getStringValue(root, "product", "product_name", "商品", "商品名");
        int quantity = getIntValue(root, "quantity", "qty", "数量");
        float unitPrice = getFloatValue(root, "unit_price", "price", "单价");
        
        if (!name.isEmpty() && quantity > 0) {
            return new ProductInfo(name, quantity, Math.max(0, unitPrice));
        }
        
        return null;
    }

    /**
     * 从文本中提取商品信息 - 正则表达式方法
     */
    private ProductInfo extractProductFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        // 大幅扩展商品名提取：涵盖更多常见商品
        String[] productPatterns = {
            // 🆕 书籍类（新增）
            "(教材|课本|书籍|书本|图书|杂志|期刊|字典|词典|书|小说|文学|[\\u4e00-\\u9fa5]{1,8}书)",
            
            // 🆕 电子产品类（新增）- 优先匹配更具体的名称
            "(服务器|路由器|交换机|投影仪|扫描仪|打印机)",  // 优先级1：最具体的设备
            "(笔记本|台式机|显示器|键盘|鼠标|音响|耳机|手机|平板)",  // 优先级2：具体设备
            "(电脑|计算机)",  // 优先级3：通用计算设备
            
            // 饮品类
            "(水|饮用水|矿泉水|纯净水|饮料|可乐|雪碧|果汁|茶|咖啡|奶茶|豆浆)",
            
            // 水果类
            "(苹果|橙子|香蕉|梨子|葡萄|西瓜|草莓|芒果|桃子|樱桃|柠檬|橘子|柚子|猕猴桃|火龙果|榴莲)",
            
            // 主食类
            "(大米|面粉|面条|馒头|包子|饺子|汤圆|米饭|面包|饼干|蛋糕|粥|粉条|河粉|方便面)",
            
            // 乳制品类
            "(鸡蛋|牛奶|酸奶|奶酪|黄油|奶粉|豆奶|酸奶|乳制品)",
            
            // 肉类
            "(鱼|肉|鸡|鸭|猪肉|牛肉|羊肉|火腿|香肠|腊肉|培根|鸡翅|鸡腿|排骨)",
            
            // 蔬菜类
            "(青菜|白菜|萝卜|土豆|西红柿|黄瓜|茄子|豆角|辣椒|洋葱|蒜|姜|韭菜|菠菜|芹菜)",
            
            // 日用品类
            "(纸巾|卫生纸|洗发水|沐浴露|牙膏|牙刷|毛巾|香皂|洗衣粉|洗洁精)",
            
            // 🆕 办公用品类（新增）
            "(桌子|椅子|文件柜|书架|白板|投影屏|办公桌|会议桌|复印纸|笔|本子|文件夹)",
            
            // 🆕 家具家电类（新增）
            "(冰箱|洗衣机|空调|电视|沙发|床|衣柜|餐桌|微波炉|电饭煲|热水器)",
            
            // 通用商品词
            "([\\u4e00-\\u9fa5]{1,4}(?:商品|产品|货物|物品|用品))",  // XX商品、XX产品等
            "([\\u4e00-\\u9fa5]{2,6})"  // 2-6个中文字符的通用商品名
        };
        
        String productName = "";
        for (String pattern : productPatterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) {
                String candidate = m.group(1);
                // 添加更严格的商品名验证
                if (isValidProductName(candidate)) {
                    productName = candidate;
                    break;
                }
            }
        }
        
        // 🆕 特殊处理：组合式书名，如"嵌入式书"、"Java编程书"等
        if (productName.isEmpty() && text.contains("书")) {
            String[] bookPatterns = {
                "([\\u4e00-\\u9fa5a-zA-Z0-9]{1,10}\\s*书)",  // 任何词+书
                "([\\u4e00-\\u9fa5a-zA-Z0-9]{1,10}\\s*图书)",
                "([\\u4e00-\\u9fa5a-zA-Z0-9]{1,10}\\s*教材)",
                "(关于[\\u4e00-\\u9fa5a-zA-Z0-9]{1,10}的书)"
            };
            
            for (String pattern : bookPatterns) {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher m = p.matcher(text);
                if (m.find()) {
                    productName = m.group(1);
                    System.out.println("🔍 提取到特殊书籍名: " + productName);
                    break;
                }
            }
        }
        
        if (productName.isEmpty()) {
            return null;
        }
        
        // 大幅优化数量提取：支持更多表达方式
        int quantity = 0;
        String[] quantityPatterns = {
            // 书籍专用模式
            "(\\d+)\\s*本\\s*" + productName,               // 10本书
            productName + "\\s*(\\d+)\\s*本",               // 书10本
            "买了\\s*(\\d+)\\s*本\\s*" + productName,       // 买了10本书
            "买\\s*(\\d+)\\s*本\\s*" + productName,         // 买10本书
            "(\\d+)\\s*本",                                 // 10本(后跟其他文字)
            "([一二三四五六七八九十百]+)\\s*本",              // 十本
            
            // 基础数量模式
            "(\\d+)\\s*个\\s*" + productName,               // 5个水
            "(\\d+)\\s*瓶\\s*" + productName,               // 5瓶水
            "(\\d+)\\s*件\\s*" + productName,               // 5件商品
            "(\\d+)\\s*只\\s*" + productName,               // 5只鸡
            "(\\d+)\\s*袋\\s*" + productName,               // 5袋大米
            "(\\d+)\\s*箱\\s*" + productName,               // 5箱饮料
            "(\\d+)\\s*斤\\s*" + productName,               // 5斤苹果
            "(\\d+)\\s*公斤\\s*" + productName,             // 5公斤米
            "(\\d+)\\s*台\\s*" + productName,               // 🆕 5台电脑
            "(\\d+)\\s*部\\s*" + productName,               // 🆕 5部手机
            "(\\d+)\\s*套\\s*" + productName,               // 🆕 5套设备
            "(\\d+)\\s*张\\s*" + productName,               // 🆕 5张桌子
            "(\\d+)\\s*把\\s*" + productName,               // 🆕 5把椅子
            
            // 🆕 新增：数字+单位+商品的模式
            "([一二三四五六七八九十]|\\d+)\\s*瓶\\s*" + productName,     // 三瓶水
            "([一二三四五六七八九十]|\\d+)\\s*个\\s*" + productName,      // 五个苹果
            "([一二三四五六七八九十]|\\d+)\\s*件\\s*" + productName,      // 十件商品
            "([一二三四五六七八九十]|\\d+)\\s*只\\s*" + productName,      // 两只鸡
            "([一二三四五六七八九十]|\\d+)\\s*袋\\s*" + productName,      // 一袋米
            "([一二三四五六七八九十]|\\d+)\\s*箱\\s*" + productName,      // 六箱饮料
            "([一二三四五六七八九十]|\\d+)\\s*台\\s*" + productName,      // 🆕 一百台电脑
            "([一二三四五六七八九十]|\\d+)\\s*部\\s*" + productName,      // 🆕 五部手机
            "([一二三四五六七八九十]|\\d+)\\s*套\\s*" + productName,      // 🆕 三套设备
            "([一二三四五六七八九十]|\\d+)\\s*张\\s*" + productName,      // 🆕 十张桌子
            "([一二三四五六七八九十]|\\d+)\\s*把\\s*" + productName,      // 🆕 五把椅子
            
            // 倒序模式：商品+数量
            productName + "\\s*(\\d+)\\s*个",               // 水5个
            productName + "\\s*(\\d+)\\s*瓶",               // 水5瓶
            productName + "\\s*(\\d+)\\s*件",               // 商品5件
            productName + "\\s*(\\d+)\\s*台",               // 🆕 电脑5台
            productName + "\\s*(\\d+)\\s*部",               // 🆕 手机5部
            productName + "\\s*(\\d+)\\s*套",               // 🆕 设备5套
            
            // 灵活的中文表达
            "(\\d+)\\s*" + productName,                     // 5水（简化表达）
            productName + "\\s*(\\d+)",                     // 水5（简化表达）
            "买\\s*(\\d+)\\s*" + productName,              // 买5个水
            "要\\s*(\\d+)\\s*" + productName,              // 要5瓶水
            "需要\\s*(\\d+)\\s*" + productName,            // 需要5件商品
            "买了\\s*(\\d+)\\s*台\\s*" + productName,       // 🆕 买了100台电脑
            "买了\\s*(\\d+)\\s*部\\s*" + productName,       // 🆕 买了5部手机
            "买了\\s*(\\d+)\\s*套\\s*" + productName,       // 🆕 买了3套设备
            
            // 通用数量模式
            "数量\\s*(\\d+)",                               // 数量5
            "(\\d+)\\s*(?:个|瓶|件|只|袋|箱|斤|公斤|台|部|套|张|把|本)",      // 🆕 扩展单位，包括"本"
        };
        
        for (String pattern : quantityPatterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) {
                try {
                    String quantityStr = m.group(1);
                    // 处理中文数字转换
                    quantity = convertChineseNumber(quantityStr);
                    if (quantity > 0) {
                        break; // 找到有效数量就停止
                    }
                } catch (NumberFormatException e) {
                    // 忽略解析错误，继续尝试下一个模式
                }
            }
        }
        
        // 大幅优化单价提取：支持更多价格表达
        float unitPrice = 0.0f;
        String[] pricePatterns = {
            // "一瓶X元"、"每个X元"模式
            "一\\s*瓶\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 一瓶3元
            "一\\s*个\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 一个5元
            "一\\s*件\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 一件10元
            "一\\s*只\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 一只20元
            "一\\s*袋\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 一袋30元
            "一\\s*斤\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 一斤8元
            
            "每\\s*瓶\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 每瓶3元
            "每\\s*个\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 每个5元
            "每\\s*件\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 每件10元
            "每\\s*只\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 每只20元
            "每\\s*袋\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 每袋30元
            "每\\s*斤\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 每斤8元
            
            // 基础价格模式
            "(\\d+(?:\\.\\d+)?)\\s*元\\s*一",                // 3元一瓶
            "(\\d+(?:\\.\\d+)?)\\s*块\\s*一",                // 3块一个
            "(\\d+(?:\\.\\d+)?)\\s*钱\\s*一",                // 3钱一件
            
            // 标准价格模式
            "(\\d+(?:\\.\\d+)?)\\s*元",                      // 3元
            "(\\d+(?:\\.\\d+)?)\\s*块",                      // 3块
            "(\\d+(?:\\.\\d+)?)\\s*钱",                      // 3钱
            "单价\\s*(\\d+(?:\\.\\d+)?)",                    // 单价3
            "价格\\s*(\\d+(?:\\.\\d+)?)",                    // 价格3
            
            // 通用价格模式
            "([0-9]+(?:\\.[0-9]+)?)\\s*(?:元|块|钱|￥|¥)",   // 支持￥符号
            
            // 增强的商品价格模式
            productName + "\\s*单价\\s*(\\d+(?:\\.\\d+)?)",  // 水单价3
            productName + "\\s*(\\d+(?:\\.\\d+)?)\\s*元",    // 水3元
            "单价\\s*(\\d+(?:\\.\\d+)?)(?:/|每|每个|每瓶|每件)",  // 单价3/个
            "价格\\s*(\\d+(?:\\.\\d+)?)(?:/|每|每个|每瓶|每件)",  // 价格3/个
            "(?:售价|卖|卖价)\\s*(\\d+(?:\\.\\d+)?)",         // 售价3、卖3
            
            // 仅价格补充模式
            "^\\s*单价\\s*(\\d+(?:\\.\\d+)?)",               // 单价3（仅价格信息）
            "^\\s*(\\d+(?:\\.\\d+)?)\\s*元?/?(?:个|瓶|件|只|袋|箱|斤)",  // 3/个（仅价格信息）
            "^\\s*每(?:个|瓶|件|只|袋|箱|斤)\\s*(\\d+(?:\\.\\d+)?)",     // 每个3（仅价格信息）
        };
        
        for (String pattern : pricePatterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) {
                try {
                    unitPrice = Float.parseFloat(m.group(1));
                    if (unitPrice >= 0) {
                        break; // 找到有效价格就停止
                    }
                } catch (NumberFormatException e) {
                    // 忽略解析错误，继续尝试下一个模式
                }
            }
        }
        
        // 如果至少有商品名和数量，就创建商品信息
        if (!productName.isEmpty() && quantity > 0) {
            System.out.println(String.format("🛒 成功提取商品信息: %s × %d @ ¥%.2f", productName, quantity, unitPrice));
            return new ProductInfo(productName, quantity, unitPrice);
        }
        
        return null;
    }
    
    /**
     * 验证商品名是否有效
     */
    private boolean isValidProductName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        // 过滤明显不是商品的词汇
        String[] invalidProducts = {
            "创建", "订单", "查询", "删除", "买", "卖", "购买", "销售",
            "客户", "供应商", "数量", "单价", "价格", "元", "块", "钱",
            "个", "件", "只", "瓶", "袋", "箱", "斤", "公斤", "那里", "这里", "处",
            // 数量+单位组合
            "一瓶", "一个", "一件", "一只", "一袋", "一箱", "一斤", "三瓶", "五个", "十件"
        };
        
        String lowerName = name.toLowerCase();
        for (String invalid : invalidProducts) {
            if (lowerName.equals(invalid) || lowerName.equals(invalid.toLowerCase())) {
                return false;
            }
        }
        
        // 检查长度：商品名应该在合理范围内
        if (name.length() < 1 || name.length() > 10) {
            return false;
        }
        
        // 检查是否只包含数字
        if (name.matches("^\\d+$")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 将中文数字转换为阿拉伯数字
     */
    private int convertChineseNumber(String chineseNumber) {
        if (chineseNumber == null || chineseNumber.trim().isEmpty()) {
            return 0;
        }
        
        // 如果已经是阿拉伯数字，直接解析
        try {
            return Integer.parseInt(chineseNumber.trim());
        } catch (NumberFormatException e) {
            // 不是阿拉伯数字，继续处理中文数字
        }
        
        // 中文数字映射
        String chineseNum = chineseNumber.trim();
        switch (chineseNum) {
            case "一": return 1;
            case "二": return 2;
            case "三": return 3;
            case "四": return 4;
            case "五": return 5;
            case "六": return 6;
            case "七": return 7;
            case "八": return 8;
            case "九": return 9;
            case "十": return 10;
            case "十一": return 11;
            case "十二": return 12;
            case "十三": return 13;
            case "十四": return 14;
            case "十五": return 15;
            case "十六": return 16;
            case "十七": return 17;
            case "十八": return 18;
            case "十九": return 19;
            case "二十": return 20;
            // 🆕 新增更大数字支持
            case "三十": return 30;
            case "四十": return 40;
            case "五十": return 50;
            case "六十": return 60;
            case "七十": return 70;
            case "八十": return 80;
            case "九十": return 90;
            case "一百": return 100;
            case "二百": return 200;
            case "三百": return 300;
            case "四百": return 400;
            case "五百": return 500;
            case "六百": return 600;
            case "七百": return 700;
            case "八百": return 800;
            case "九百": return 900;
            case "一千": return 1000;
            default:
                // 🆕 支持组合数字如"二十三"、"一百五十"等
                return parseComplexChineseNumber(chineseNum);
        }
    }
    
    /**
     * 🆕 解析复杂的中文数字组合
     */
    private int parseComplexChineseNumber(String chineseNum) {
        try {
            // 处理"XX十Y"格式，如"二十三"
            if (chineseNum.contains("十") && chineseNum.length() <= 3) {
                if (chineseNum.startsWith("十")) {
                    // "十三" = 13
                    String remainder = chineseNum.substring(1);
                    return 10 + convertSingleDigit(remainder);
                } else {
                    // "二十三" = 23
                    String[] parts = chineseNum.split("十");
                    if (parts.length == 2) {
                        int tens = convertSingleDigit(parts[0]) * 10;
                        int ones = parts[1].isEmpty() ? 0 : convertSingleDigit(parts[1]);
                        return tens + ones;
                    }
                }
            }
            
            // 处理"XX百YY"格式，如"一百五十"
            if (chineseNum.contains("百")) {
                String[] parts = chineseNum.split("百");
                if (parts.length >= 1) {
                    int hundreds = convertSingleDigit(parts[0]) * 100;
                    if (parts.length == 2 && !parts[1].isEmpty()) {
                        int remainder = parseComplexChineseNumber(parts[1]);
                        return hundreds + remainder;
                    }
                    return hundreds;
                }
            }
            
            // 处理"XX千YYY"格式
            if (chineseNum.contains("千")) {
                String[] parts = chineseNum.split("千");
                if (parts.length >= 1) {
                    int thousands = convertSingleDigit(parts[0]) * 1000;
                    if (parts.length == 2 && !parts[1].isEmpty()) {
                        int remainder = parseComplexChineseNumber(parts[1]);
                        return thousands + remainder;
                    }
                    return thousands;
                }
            }
            
        } catch (Exception e) {
            // 解析失败，返回0
        }
        
        // 无法解析的复杂数字，返回0
        return 0;
    }
    
    /**
     * 🆕 转换单个中文数字字符
     */
    private int convertSingleDigit(String digit) {
        switch (digit) {
            case "一": return 1;
            case "二": return 2;
            case "三": return 3;
            case "四": return 4;
            case "五": return 5;
            case "六": return 6;
            case "七": return 7;
            case "八": return 8;
            case "九": return 9;
            default: return 0;
        }
    }
} 