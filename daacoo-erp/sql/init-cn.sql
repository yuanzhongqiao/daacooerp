-- 创建数据库
-- Create DataBase daacooerp
CREATE DATABASE IF NOT EXISTS daacooerp CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE daacooerp;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    tel VARCHAR(20) COMMENT '电话号码',
    email VARCHAR(100) COMMENT '邮箱',
    role VARCHAR(20) DEFAULT 'user' COMMENT '角色：admin-管理员，user-普通用户',
    status TINYINT(1) DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    last_login DATETIME COMMENT '最后登录时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `idx_username` (username),
    UNIQUE KEY `idx_tel` (tel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商品表
CREATE TABLE IF NOT EXISTS goods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL COMMENT '商品编码',
    name VARCHAR(100) NOT NULL COMMENT '商品名称',
    category VARCHAR(50) COMMENT '商品类别',
    specification VARCHAR(100) COMMENT '规格',
    unit VARCHAR(20) COMMENT '单位',
    purchase_price DECIMAL(10,2) COMMENT '采购价',
    selling_price DECIMAL(10,2) COMMENT '销售价',
    stock INT DEFAULT 0 COMMENT '库存数量',
    status TINYINT(1) DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    description TEXT COMMENT '描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `idx_code` (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 客户订单表
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL COMMENT '订单编号',
    order_type VARCHAR(20) NOT NULL COMMENT '订单类型：PURCHASE-采购订单，SALE-销售订单',
    customer_name VARCHAR(100) COMMENT '客户名称',
    contact_person VARCHAR(50) COMMENT '联系人',
    tel VARCHAR(20) COMMENT '联系电话',
    address VARCHAR(255) COMMENT '地址',
    delivery_time DATETIME COMMENT '交付时间',
    amount DECIMAL(10,2) DEFAULT 0 COMMENT '订单金额',
    freight DECIMAL(10,2) DEFAULT 0 COMMENT '运费',
    operator_id BIGINT COMMENT '操作员ID',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '订单状态：PENDING-待处理，PROCESSING-处理中，COMPLETED-已完成，CANCELLED-已取消',
    remarks TEXT COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (operator_id) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE KEY `idx_order_no` (order_no),
    INDEX `idx_created_at` (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单商品表
CREATE TABLE IF NOT EXISTS order_goods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    goods_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL DEFAULT 0 COMMENT '数量',
    unit_price DECIMAL(10,2) COMMENT '单价',
    total_price DECIMAL(10,2) COMMENT '总价',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (goods_id) REFERENCES goods(id) ON DELETE RESTRICT,
    INDEX `idx_order_id` (order_id),
    INDEX `idx_goods_id` (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品表';

-- 创建库存表
CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    product_code VARCHAR(50) NOT NULL COMMENT '商品编码',
    quantity INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    unit VARCHAR(20) COMMENT '单位',
    unit_price DECIMAL(10,2) COMMENT '单价',
    location VARCHAR(100) COMMENT '库存位置',
    category VARCHAR(50) COMMENT '分类',
    description TEXT COMMENT '描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_product_code` (product_code),
    INDEX `idx_category` (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

-- 创建公司表
CREATE TABLE IF NOT EXISTS company (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '公司ID',
    name VARCHAR(100) NOT NULL COMMENT '公司名称',
    address VARCHAR(255) COMMENT '公司地址',
    contact VARCHAR(50) COMMENT '联系方式',
    email VARCHAR(100) COMMENT '电子邮箱',
    type VARCHAR(50) COMMENT '公司类型',
    contact_person VARCHAR(50) COMMENT '联系人',
    remark TEXT COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_company_name` (name),
    INDEX `idx_company_type` (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司表';

-- 创建员工表
CREATE TABLE IF NOT EXISTS staff (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '员工ID',
    name VARCHAR(50) NOT NULL COMMENT '员工姓名',
    position VARCHAR(50) COMMENT '职位',
    tel VARCHAR(20) COMMENT '联系电话',
    email VARCHAR(100) COMMENT '电子邮箱',
    department VARCHAR(50) COMMENT '部门',
    join_date DATE COMMENT '入职日期',
    status VARCHAR(20) DEFAULT '在职' COMMENT '状态',
    company_id BIGINT COMMENT '所属公司ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (company_id) REFERENCES company(id) ON DELETE SET NULL,
    INDEX `idx_staff_company` (company_id),
    INDEX `idx_staff_department` (department)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

-- 创建财务数据表
CREATE TABLE IF NOT EXISTS finance_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '财务记录ID',
    record_date DATE NOT NULL COMMENT '记录日期',
    income DECIMAL(12,2) DEFAULT 0.00 COMMENT '收入金额',
    expense DECIMAL(12,2) DEFAULT 0.00 COMMENT '支出金额',
    profit DECIMAL(12,2) DEFAULT 0.00 COMMENT '利润',
    record_type VARCHAR(20) COMMENT '记录类型',
    description VARCHAR(255) COMMENT '描述',
    created_by VARCHAR(50) COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_record_date` (record_date),
    INDEX `idx_record_type` (record_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财务记录表'; 

-- 初始化管理员账户
INSERT INTO users (username, password, tel, email, role, status, created_at, updated_at)
VALUES ('admin', '$2a$10$oZUahFN8H9T8RbCtq3cRH.UI/HyWupN8drIgO78kOed20.6EQbC52', '13800000000', 'admin@daacooerp.com', 'admin', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
password = VALUES(password),
updated_at = CURRENT_TIMESTAMP;

-- 注意：密码为 'admin123'，使用BCrypt加密
ALTER TABLE `users`
  ADD COLUMN `avatar` VARCHAR(255) NULL COMMENT '头像 URL';