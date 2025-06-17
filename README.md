# DaaCoo ERP System  
[中文](https://github.com/yuanzhongqiao/daacooerp/blob/main/README-cn.md)

> A modern ERP system with AI natural language interaction by DaaCoo  

Developed based on DaaCoo, integrated with DeepSeek AI, allowing you to create orders using natural language.  

# DaaCoo is the Platform

This is a system by Daacoo The LowCode  LLM-naitve-Platform for Enterpise Application

www.daacoo.com  


## ✨ DaaCooERP Core Features  

🤖 **AI-Powered Order Creation** - Create orders by speaking naturally:  
"Purchase 20 mice from JD.com at 50 yuan each"  
"Order 20 mobile phones from Huawei"  

📊 **Complete ERP Functionality** - Covers the full lifecycle management of procurement, sales, inventory, and finance.  

🎨 **Modern Interface** - Beautiful responsive design based on Element Plus.  

⚡ **Quick Deployment** - Frontend and backend separation, one-click Docker deployment.  

## 🚀 Quick Start  

### Environment Setup  
- Node.js 16+  
- JDK 21+  
- MySQL 8.0+  
- DeepSeek API Key  

### Launch Steps  

1. **Clone the Project**  
```bash  
git clone https://github.com/yuanzhongqiao/daacooerp.git
cd daacooerp  
```  

2. **Start the Database**  
```sql  
CREATE DATABASE daacooerp CHARACTER SET utf8mb4;  
```  

3. **Start the Backend**  
```bash  
cd daacooERP
# Configure database and AI key in application.properties  
mvn spring-boot:run  
```  

4. **Start the Frontend**  
```bash  
cd daacooERP-vue  
npm install  
npm run dev  
```  

5. **Access the System**  

Config in the VUE
- Frontend: http://localhost:5173     

Config in the Spring Boot
- Backend: http://localhost:8081  

## 🎯 AI Functionality Demo  

### Create Purchase Orders  
```  
"Purchase 200 keyboards from Alibaba at 80 yuan each"  
"Order 100 mobile phones from Huawei"  
```  

### Create Sales Orders  
```  
"Sell 50 chargers to Xiaomi Corporation at 30 yuan each"  
"Sell 100 laptops to Lenovo"  
```  

The AI will automatically identify:  
- Order type (purchase/sales)  
- Supplier/customer  
- Product name and quantity  
- Price information  

It will intelligently ask for missing information.  

## 📋 System Functions  

| Module | Function |  
|--------|----------|  
| 🏢 **Company Management** | Customer, supplier, and employee information management |  
| 📦 **Product Management** | Product archives, pricing, and inventory management |  
| 📋 **Order Management** | Purchase orders, sales orders, and AI-powered creation |  
| 📊 **Inventory Management** | Real-time inventory, inbound/outbound, and alert reminders |  
| 💰 **Financial Management** | Income/expense records, report statistics, and chart displays |  
| 🤖 **AI Assistant** | Natural language operations, intelligent parsing, and multi-round dialogue |  

## 🛠️ Technical Architecture  

```  
Frontend (Vue 3 + Vite)  
├── Element Plus UI  
├── Pinia State Management  
├── ECharts Charts  
└── Axios HTTP  

Backend (Spring Boot 3)  
├── Spring Data JPA  
├── MySQL Database  
├── JWT Security Authentication  
└── DeepSeek AI Integration  
```  

## 📁 Project Structure  

```  
daacooerp/  
├── daacooERP-vue/          # Frontend Vue Project  
│   ├── src/views/           # Page Components  
│   │   ├── dashboard/       # Dashboard  
│   │   ├── order/           # Order Management  
│   │   ├── inventory/       # Inventory Management  
│   │   ├── finance/         # Financial Management  
│   │   └── company/         # Company Management  
│   └── src/api/             # API Interfaces  
├── daacooERP-springboot/   # Backend Spring Boot  
│   ├── controller/          # Controllers  
│   ├── service/             # Business Services  
│   ├── entity/              # Data Entities  
│   └── repository/          # Data Access  
└── sql/                     # Database Scripts  
```  

## ⚙️ Configuration Instructions  

### Backend Configuration (application.properties)  
```properties  
# Database Configuration  
spring.datasource.url=jdbc:mysql://localhost:3306/daacooerp  
spring.datasource.username=daacooerp   
spring.datasource.password=daacooerp123456  

# AI Configuration  
deepseek.api.key=your_api_key_here  
```  

### Frontend Configuration (.env.development)  
```env  
VITE_API_BASE_URL=http://localhost:8081  
```  

## 🐳 Docker Deployment  

```bash  
# Build and start  
docker-compose up -d  

# Stop services  
docker-compose down  
```  

## ❓ FAQ  

**Q: AI not working?**  
A: Check if the DeepSeek API key is correctly configured.  

https://platform.deepseek.com/

**Q: Frontend cannot connect to the backend?**  
A: Confirm that the backend is running normally on port 8081.  

**Q: Database connection failed?**  
A: Check the MySQL service status and configuration information.  

## 🤝 Contribution  

1. Fork the project  
2. Create a feature branch  
3. Commit your code  
4. Initiate a Pull Request  

## 📝 Open Source License  

This project follows the PKU (LICENSE) open source license.  

[PKU (LICENSE) open source license](https://www.gitpp.com/pkuLicense/pku-open-source-license)

## 📧 Contact Us  

- Email: 274288672@qq.com  
- Issues: [Submit an Issue](../../issues)  

---  

⭐ If you find it useful, please give it a Star!
