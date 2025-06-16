package com.daacooerp.erp.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "orders")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_no", nullable = false)
    private String orderNo;
    
    @Column(name = "order_type")
    private String orderType; // PURCHASE-采购订单，SALE-销售订单
    
    @Transient // 不持久化到数据库，仅用于接收前端参数
    private String type; // 前端传递的订单类型：customer-客户订单，purchase-采购订单
    
    @Column(name = "customer_name")
    private String customerName;
    
    @Column(name = "contact_person")
    private String contactPerson;
    
    private String tel;
    private String address;
    
    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;
    
    @Column(name = "amount")
    @JsonProperty("amount")
    private Float amount = 0.0f;
    
    @Transient
    @JsonProperty("totalAmount") 
    public Float getTotalAmount() {
        return this.amount;
    }
    
    @JsonProperty("totalAmount")
    public void setTotalAmount(Float totalAmount) {
        this.amount = totalAmount;
    }
    
    private Float freight = 0.0f;
    
    @ManyToOne
    @JoinColumn(name = "operator_id")
    private User operator;
    
    private String status = "PENDING"; // 订单状态：PENDING-待处理，PROCESSING-处理中，COMPLETED-已完成，CANCELLED-已取消
    private String remarks;
    
    @Column(name = "created_at", updatable = false)
    @JsonProperty("createTime")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @JsonProperty("updateTime")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "order", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<OrderGoods> goods = new java.util.ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}