# DaaCoo ERP系统 集成DeepSeek的免费ERP

www.daacoo.com 

> 一个支持AI自然语言交互的现代化ERP系统  by DaaCoo

基于DaaCoo开发，集成DeepSeek AI，让您可以用自然语言运行 ERP。  

🤖 **LLM Native ERP**

## 📝 开源协议

本项目遵循PKU (LICENSE) 开源协议，中国人自己的开源协议
[PKU (LICENSE) 开源协议](https://www.gitpp.com/pkuLicense/pku-open-source-license)


## 📋 系统功能

| 模块 | 功能 |
|------|------|
| 🏢 **企业管理** | 客户、供应商、员工信息管理 |
| 📦 **商品管理** | 商品档案、价格、库存管理 |
| 📋 **订单管理** | 采购订单、销售订单、AI智能创建 |
| 📊 **库存管理** | 实时库存、出入库、预警提醒 |
| 💰 **财务管理** | 收支记录、报表统计、图表展示 |
| 🤖 **AI助手** | 自然语言操作、智能解析、多轮对话 |




## 🛠️ 技术架构

```
前端 (Vue 3 + Vite)
├── Element Plus UI
├── Pinia 状态管理
├── ECharts 图表
└── Axios HTTP

后端 (Spring Boot 3)
├── Spring Data JPA
├── MySQL 数据库
├── JWT 安全认证
└── DeepSeek AI 集成
```

## 📁 项目结构

```
daacooerp/
├── daacooERP-vue/          # 前端 Vue 项目
│   ├── src/views/           # 页面组件
│   │   ├── dashboard/       # 仪表板
│   │   ├── order/           # 订单管理
│   │   ├── inventory/       # 库存管理
│   │   ├── finance/         # 财务管理
│   │   └── company/         # 企业管理
│   └── src/api/             # API 接口
├── daacooERP/                # 后端 Spring Boot
│   ├── controller/          # 控制器
│   ├── service/             # 业务服务
│   ├── entity/              # 数据实体
│   └── repository/          # 数据访问
└── sql/                     # 数据库脚本
```

## ⚙️ 配置说明

### 后端配置 (application.properties)
```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/daacooerp
spring.datasource.username=daacooerp
spring.datasource.password=daacooerp123456

# AI配置
deepseek.api.key=your_api_key_here
```

### 前端配置 (.env.development)
```env
VITE_API_BASE_URL=http://localhost:8081
```

## 🐳 Docker部署

```bash
# 构建并启动
docker-compose up -d

# 停止服务
docker-compose down
```


## ✨ 核心特色

🤖 **AI智能下单** - 说人话就能创建订单："从京东采购100个鼠标，单价50元"

📊 **完整ERP功能** - 涵盖采购、销售、库存、财务全流程管理

🎨 **现代化界面** - 基于Element Plus的美观响应式设计

⚡ **快速部署** - 前后端分离，Docker一键部署

## 🚀 快速体验



### 环境准备
- Node.js 16+
- JDK 21+
- MySQL 8.0+
- DeepSeek API密钥

### 启动步骤

1. **克隆项目**
```bash
git clone https://github.com/yuanzhongqiao/daacooerp.git
cd daacooerp
```

2. **启动数据库**
```sql
CREATE DATABASE daacooerp CHARACTER SET utf8mb4;
```

3. **启动后端  daacoo-erp  其实是Spring Boot**
```bash
cd daacoo-erp
# 配置 application.properties 中的数据库和AI密钥
mvn spring-boot:run
```

4. **启动前端**
```bash
cd daacoo-ERP-vue
npm install
npm run dev
```

5. **访问系统 请自行在文件中设置相关端口**
- 前端：http://localhost:5173
- 后端：http://localhost:8081

6. **默认管理员**
   admin  admin123
   在SQL文件说明
## 🎯 AI功能演示

### 创建采购订单
```
"从拼多多采购200个键盘，单价80元"
"向华为订购100台手机"
```

### 创建销售订单  
```
"卖给小米公司50个充电器，每个30元"
"销售100台笔记本给联想"
```

AI会自动识别：
- 订单类型（采购/销售）
- 供应商/客户
- 商品名称和数量
- 价格信息

缺少信息时会智能询问补全。



## ❓ 常见问题

**Q: AI不工作？**  
A: 检查DeepSeek API密钥是否正确配置

https://platform.deepseek.com/


**Q: 前端连不上后端？**  
A: 确认后端在8081端口正常运行

**Q: 数据库连接失败？**  
A: 检查MySQL服务状态和配置信息

## 🤝 参与贡献

1. Fork 项目
2. 创建功能分支
3. 提交代码
4. 发起 Pull Request



## 📧 联系我们

- 邮箱：274288672@qq.com
- Issues：[提交问题](../../issues)

---

⭐ 觉得有用请点个Star！
