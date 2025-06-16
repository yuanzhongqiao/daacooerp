package com.daacooerp.erp.dto;

/**
 * AI请求DTO
 */
public class AIRequest {
    
    private String input;
    private boolean confirmed;
    private String analysisType;  // 分析类型：FINANCE, SALES, INVENTORY, ORDER, GENERAL
    private String dataContext;   // 数据上下文信息
    
    public AIRequest() {}
    
    public AIRequest(String input, boolean confirmed) {
        this.input = input;
        this.confirmed = confirmed;
    }
    
    public AIRequest(String input, boolean confirmed, String analysisType, String dataContext) {
        this.input = input;
        this.confirmed = confirmed;
        this.analysisType = analysisType;
        this.dataContext = dataContext;
    }
    
    public String getInput() {
        return input;
    }
    
    public void setInput(String input) {
        this.input = input;
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }
    
    public String getAnalysisType() {
        return analysisType;
    }
    
    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }
    
    public String getDataContext() {
        return dataContext;
    }
    
    public void setDataContext(String dataContext) {
        this.dataContext = dataContext;
    }
} 