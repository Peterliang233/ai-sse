package com.example.sse.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.sse.dto.ConversationDTO;
import com.example.sse.service.ConversationService;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    @Autowired
    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /**
     * 获取用户的所有会话列表
     */
    @GetMapping
    public ResponseEntity<List<ConversationDTO>> getAllConversations(@RequestParam Integer userId) {
        List<ConversationDTO> conversations = conversationService.findActiveConversations(userId);
        return ResponseEntity.ok(conversations);
    }

    /**
     * 获取特定会话
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConversationDTO> getConversation(@PathVariable Integer id, 
                                                          @RequestParam Integer userId) {
        ConversationDTO conversation = conversationService.findConversation(id, userId);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(conversation);
    }

    /**
     * 创建新会话
     */
    @PostMapping
    public ResponseEntity<ConversationDTO> createConversation(@RequestBody Map<String, Object> payload) {
        Integer userId = (Integer) payload.get("userId");
        String title = (String) payload.get("title");
        
        if (userId == null || title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        ConversationDTO newConversation = conversationService.createConversation(userId, title);
        return ResponseEntity.status(HttpStatus.CREATED).body(newConversation);
    }

    /**
     * 更新会话标题
     */
    @PutMapping("/{id}/title")
    public ResponseEntity<ConversationDTO> updateTitle(@PathVariable Integer id, 
                                                      @RequestBody Map<String, Object> payload) {
        Integer userId = (Integer) payload.get("userId");
        String title = (String) payload.get("title");
        
        if (userId == null || title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        ConversationDTO updatedConversation = conversationService.updateTitle(id, userId, title);
        if (updatedConversation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedConversation);
    }

    /**
     * 归档会话
     */
    @PutMapping("/{id}/archive")
    public ResponseEntity<Void> archiveConversation(@PathVariable Integer id, 
                                                    @RequestParam Integer userId) {
        boolean success = conversationService.archiveConversation(id, userId);
        if (!success) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Integer id, 
                                                  @RequestParam Integer userId) {
        boolean success = conversationService.deleteConversation(id, userId);
        if (!success) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }
} 