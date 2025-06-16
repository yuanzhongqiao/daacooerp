package com.daacooerp.erp.dto;

/**
 * AI响应DTO
 */
public class AIResponse {
    
    private String reply;
    private boolean needConfirm;
    
    public AIResponse() {}
    
    public AIResponse(String reply, boolean needConfirm) {
        this.reply = reply;
        this.needConfirm = needConfirm;
    }
    
    public String getReply() {
        return reply;
    }
    
    public void setReply(String reply) {
        this.reply = reply;
    }
    
    public boolean isNeedConfirm() {
        return needConfirm;
    }
    
    public void setNeedConfirm(boolean needConfirm) {
        this.needConfirm = needConfirm;
    }
} 