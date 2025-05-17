package com.example.sse.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.sse.dto.MessageDTO;
import com.example.sse.service.MessageService;
import com.example.sse.service.OllamaService;
import com.example.sse.service.SecurityService;
import com.example.sse.service.SseService;

@RestController
@RequestMapping("/api/sse")
public class SseController {

    private final SecurityService securityService;
    private final MessageService messageService;
    private final SseService sseService;
    private final OllamaService ollamaService;

    @Autowired
    public SseController(SseService sseService, MessageService messageService, SecurityService securityService, OllamaService ollamaService) {
        this.securityService = securityService;
        this.sseService = sseService;
        this.messageService = messageService;
        this.ollamaService = ollamaService;
    }

    /**
     * 创建SSE连接
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return sseService.createEmitter();
    }

    /**
     * 触发测试事件
     */
    @GetMapping("/trigger")
    public void triggerEvent() {
        sseService.sendToAll("测试事件", "这是一个测试事件 " + System.currentTimeMillis());
    }
    
    /**
     * 发送消息 - 旧API，保留兼容性
     */
    @PostMapping("/sendMessage")
    public void sendMessage(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        Integer userId = getCurrentUserId(); // 从认证信息中获取实际用户ID
        
        if (message != null && !message.isEmpty()) {
            // 处理旧版本的简单消息（不关联会话）
            sseService.processUserMessage(message);
        }
    }
    
    /**
     * 发送用户消息到会话 - 新API，支持流式响应
     */
    @PostMapping("/chat")
    public MessageDTO sendChatMessage(@RequestBody Map<String, Object> payload) {
        Integer conversationId = (Integer) payload.get("conversationId");
        Integer userId = (Integer) payload.get("userId");
        String content = (String) payload.get("content");
        
        if (conversationId == null || userId == null || content == null || content.isEmpty()) {
            throw new IllegalArgumentException("缺少必要参数");
        }
        
        // 添加用户消息
        MessageDTO userMessageDTO = messageService.addUserMessage(conversationId, userId, content);
        
        // 使用流式API生成AI响应，响应将通过SSE传递给客户端
        ollamaService.generateConversationResponse(content, conversationId, messageService, sseService);
        
        // 返回用户消息对象，AI的响应会通过SSE推送
        return userMessageDTO;
    }
    
    /**
     * 获取AI模型简介
     */
    @GetMapping("/ai-intro")
    public Map<String, String> getAiIntroduction() {
        String intro = ollamaService.generateIntroductionResponse();
        return Map.of("introduction", intro);
    }

    /**
     * 从安全上下文中获取当前用户ID
     */
    private Integer getCurrentUserId() {
        return securityService.getCurrentUserId();
    }
}