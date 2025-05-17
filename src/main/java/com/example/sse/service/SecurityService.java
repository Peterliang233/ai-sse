package com.example.sse.service;

public interface SecurityService {
    /**
     * 从安全上下文中获取当前用户ID
     */
    Integer getCurrentUserId();
}