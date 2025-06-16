package com.daacooerp.erp.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "order_goods")
public class OrderGoods {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonBackReference
    private Order order;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "goods_id")
    private Goods goods;
    
    private Integer quantity;
    
    @Column(name = "unit_price")
    @JsonProperty("unitPrice")
    private Float unitPrice;
    
    @Column(name = "total_price")
    @JsonProperty("totalPrice")
    private Float totalPrice;
    
    @JsonProperty("price")
    public void setPrice(Float price) {
        this.unitPrice = price;
    }
    
    @JsonProperty("price")
    public Float getPrice() {
        return this.unitPrice;
    }
    
    @JsonProperty("amount")
    public void setAmount(Float amount) {
        this.totalPrice = amount;
    }
    
    @JsonProperty("amount")
    public Float getAmount() {
        return this.totalPrice;
    }
    
    @JsonProperty("name")
    public String getName() {
        return goods != null ? goods.getName() : "";
    }
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
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