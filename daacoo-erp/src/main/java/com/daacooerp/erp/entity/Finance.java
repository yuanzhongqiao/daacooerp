package com.daacooerp.erp.entity;

import java.math.BigDecimal; // Keep for other potential uses, or remove if not needed elsewhere
import java.util.List;

public class Finance {
    private List<Float> profit;
    private List<Float> turnover; // This is usually total income
    private List<Integer> salesOrderQuantity;
    private List<Integer> purchaseOrderQuantity;
    private List<Float> salesTotalAmounts;    // Monthly total amount for sales orders
    private List<Float> purchaseTotalAmounts; // Monthly total amount for purchase orders

    public Finance() {
    }

    public Finance(List<Float> profit, List<Float> turnover, 
                   List<Integer> salesOrderQuantity, List<Integer> purchaseOrderQuantity, 
                   List<Float> salesTotalAmounts, List<Float> purchaseTotalAmounts) {
        this.profit = profit;
        this.turnover = turnover;
        this.salesOrderQuantity = salesOrderQuantity;
        this.purchaseOrderQuantity = purchaseOrderQuantity;
        this.salesTotalAmounts = salesTotalAmounts;
        this.purchaseTotalAmounts = purchaseTotalAmounts;
    }

    public List<Float> getProfit() {
        return profit;
    }

    public void setProfit(List<Float> profit) {
        this.profit = profit;
    }

    public List<Float> getTurnover() {
        return turnover;
    }

    public void setTurnover(List<Float> turnover) {
        this.turnover = turnover;
    }

    public List<Integer> getSalesOrderQuantity() {
        return salesOrderQuantity;
    }

    public void setSalesOrderQuantity(List<Integer> salesOrderQuantity) {
        this.salesOrderQuantity = salesOrderQuantity;
    }

    public List<Integer> getPurchaseOrderQuantity() {
        return purchaseOrderQuantity;
    }

    public void setPurchaseOrderQuantity(List<Integer> purchaseOrderQuantity) {
        this.purchaseOrderQuantity = purchaseOrderQuantity;
    }

    public List<Float> getSalesTotalAmounts() {
        return salesTotalAmounts;
    }

    public void setSalesTotalAmounts(List<Float> salesTotalAmounts) {
        this.salesTotalAmounts = salesTotalAmounts;
    }

    public List<Float> getPurchaseTotalAmounts() {
        return purchaseTotalAmounts;
    }

    public void setPurchaseTotalAmounts(List<Float> purchaseTotalAmounts) {
        this.purchaseTotalAmounts = purchaseTotalAmounts;
    }
}