package com.example.sse.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sse.dto.MessageDTO;
import com.example.sse.model.Conversation;
import com.example.sse.model.Message;
import com.example.sse.repository.ConversationRepository;
import com.example.sse.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final SseService sseService;
    private final OllamaService ollamaService;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    
    @Autowired
    public MessageService(SseService sseService, OllamaService ollamaService, 
                          MessageRepository messageRepository, 
                          ConversationRepository conversationRepository) {
        this.sseService = sseService;
        this.ollamaService = ollamaService;
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
    }

    /**
     * 获取会话的所有消息
     */
    @Transactional(readOnly = true)
    public List<MessageDTO> getMessagesForConversation(Integer conversationId, Integer userId) {
        // 验证会话是否存在且属于该用户
        Optional<Conversation> conversation = conversationRepository.findById(conversationId);
        if (conversation.isEmpty() || !conversation.get().getUserId().equals(userId)) {
            return new ArrayList<>();
        }
        
        // 查询消息
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        
        // 转换为DTO
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 处理用户消息（旧方法，保留兼容性）
     */
    public void processUserMessage(String message, String sessionId) {
        // 使用Ollama生成回复
        String aiResponse = ollamaService.generateResponse(message);
        
        // 通知所有客户端有新的AI回复
        Map<String, Object> aiMessage = new HashMap<>();
        aiMessage.put("id", UUID.randomUUID().toString());
        aiMessage.put("content", aiResponse);
        aiMessage.put("role", "assistant");
        aiMessage.put("timestamp", System.currentTimeMillis());
        
        sseService.sendToAll("newMessage", aiMessage);
    }

    /**
     * 添加用户消息
     */
    @Transactional
    public MessageDTO addUserMessage(Integer conversationId, Integer userId, String content) {
        // 验证参数
        if (conversationId == null || userId == null || content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid parameters");
        }
        
        // 查找会话
        Optional<Conversation> optionalConversation = conversationRepository.findById(conversationId);
        if (optionalConversation.isEmpty()) {
            throw new IllegalArgumentException("Conversation not found");
        }
        
        Conversation conversation = optionalConversation.get();
        
        // 创建并保存消息实体
        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderType(Message.SENDER_USER);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        
        Message savedMessage = messageRepository.save(message);
        
        // 更新会话最后更新时间
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        
        // 转换为DTO
        MessageDTO messageDTO = convertToDTO(savedMessage);
        
        // 通知所有客户端有新消息
        sseService.sendToAll("新消息", messageDTO);
        
        return messageDTO;
    }

    /**
     * 添加AI回复
     */
    @Transactional
    public MessageDTO addAiResponse(Integer conversationId, String content) {
        // 验证参数
        if (conversationId == null || content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid parameters");
        }
        
        // 查找会话
        Optional<Conversation> optionalConversation = conversationRepository.findById(conversationId);
        if (optionalConversation.isEmpty()) {
            throw new IllegalArgumentException("Conversation not found");
        }
        
        Conversation conversation = optionalConversation.get();
        
        // 创建并保存消息实体
        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderType(Message.SENDER_AI);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        
        Message savedMessage = messageRepository.save(message);
        
        // 更新会话最后更新时间
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        
        // 转换为DTO
        MessageDTO messageDTO = convertToDTO(savedMessage);
        
        // 通知所有客户端有新的AI消息
        sseService.sendToAll("AI响应", messageDTO);
        
        return messageDTO;
    }
    
    /**
     * 将消息实体转换为DTO
     */
    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation().getId());
        dto.setContent(message.getContent());
        dto.setSenderType(message.getSenderType());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
}