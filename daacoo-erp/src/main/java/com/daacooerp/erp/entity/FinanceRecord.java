package com.daacooerp.erp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "finance_record")
public class FinanceRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "record_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date recordDate;
    
    @Column(name = "income", precision = 12, scale = 2)
    private BigDecimal income = BigDecimal.ZERO;
    
    @Column(name = "expense", precision = 12, scale = 2)
    private BigDecimal expense = BigDecimal.ZERO;
    
    @Column(name = "profit", precision = 12, scale = 2)
    private BigDecimal profit = BigDecimal.ZERO;
    
    @Column(name = "record_type")
    private String recordType;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    
    // 构造函数
    public FinanceRecord() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Date getRecordDate() {
        return recordDate;
    }
    
    public void setRecordDate(Date recordDate) {
        this.recordDate = recordDate;
    }
    
    public BigDecimal getIncome() {
        return income;
    }
    
    public void setIncome(BigDecimal income) {
        this.income = income;
    }
    
    public BigDecimal getExpense() {
        return expense;
    }
    
    public void setExpense(BigDecimal expense) {
        this.expense = expense;
    }
    
    public BigDecimal getProfit() {
        return profit;
    }
    
    public void setProfit(BigDecimal profit) {
        this.profit = profit;
    }
    
    public String getRecordType() {
        return recordType;
    }
    
    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // 在保存前计算利润
    @PrePersist
    @PreUpdate
    public void calculateProfit() {
        if (this.income != null && this.expense != null) {
            this.profit = this.income.subtract(this.expense);
        }
    }
} 