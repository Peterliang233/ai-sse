package com.example.sse.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.sse.dto.MessageDTO;
import com.example.sse.service.MessageService;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    @Autowired
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 获取会话的所有消息
     */
    @GetMapping
    public ResponseEntity<List<MessageDTO>> getMessagesForConversation(
            @RequestParam Integer conversationId,
            @RequestParam Integer userId) {
        
        List<MessageDTO> messages = messageService.getMessagesForConversation(conversationId, userId);
        return ResponseEntity.ok(messages);
    }

    /**
     * 发送用户消息
     */
    @PostMapping("/user")
    public ResponseEntity<MessageDTO> sendUserMessage(@RequestBody Map<String, Object> payload) {
        Integer conversationId = (Integer) payload.get("conversationId");
        Integer userId = (Integer) payload.get("userId");
        String content = (String) payload.get("content");
        
        if (conversationId == null || userId == null || content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        MessageDTO messageDTO = messageService.addUserMessage(conversationId, userId, content);
        if (messageDTO == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(messageDTO);
    }

    /**
     * 手动发送AI响应（通常由后台服务自动处理）
     * 仅用于测试或管理目的
     */
    @PostMapping("/ai")
    public ResponseEntity<MessageDTO> sendAiResponse(@RequestBody Map<String, Object> payload) {
        Integer conversationId = (Integer) payload.get("conversationId");
        String content = (String) payload.get("content");
        
        if (conversationId == null || content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        MessageDTO messageDTO = messageService.addAiResponse(conversationId, content);
        if (messageDTO == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(messageDTO);
    }
} 