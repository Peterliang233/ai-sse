package com.example.sse.service;

import com.example.sse.dto.ConversationDTO;
import com.example.sse.model.Conversation;
import com.example.sse.model.Message;
import com.example.sse.repository.ConversationRepository;
import com.example.sse.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversationService {
    
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    
    @Autowired
    public ConversationService(ConversationRepository conversationRepository, 
                               MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }
    
    /**
     * 查找用户的所有活跃会话
     */
    public List<ConversationDTO> findActiveConversations(Integer userId) {
        List<Conversation> conversations = conversationRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, 1);
        
        return conversations.stream().map(conv -> {
            ConversationDTO dto = convertToDTO(conv);
            // 获取消息数量
            Long messageCount = messageRepository.countByConversationId(conv.getId());
            dto.setMessageCount(messageCount.intValue());
            
            // 获取最后一条消息作为预览
            List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId());
            if (!messages.isEmpty()) {
                Message lastMessage = messages.get(messages.size() - 1);
                // 缩短显示
                String preview = lastMessage.getContent();
                if (preview.length() > 50) {
                    preview = preview.substring(0, 47) + "...";
                }
                dto.setLastMessage(preview);
            }
            
            return dto;
        }).collect(Collectors.toList());
    }
    
    /**
     * 创建新会话
     */
    @Transactional
    public ConversationDTO createConversation(Integer userId, String title) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(title);
        conversation.setStatus(1);
        
        Conversation saved = conversationRepository.save(conversation);
        return convertToDTO(saved);
    }
    
    /**
     * 查找特定会话
     */
    public ConversationDTO findConversation(Integer id, Integer userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(id, userId);
        if (conversation == null) {
            return null;
        }
        
        return convertToDTO(conversation);
    }
    
    /**
     * 更新会话标题
     */
    @Transactional
    public ConversationDTO updateTitle(Integer id, Integer userId, String title) {
        Conversation conversation = conversationRepository.findByIdAndUserId(id, userId);
        if (conversation == null) {
            return null;
        }
        
        conversation.setTitle(title);
        Conversation saved = conversationRepository.save(conversation);
        return convertToDTO(saved);
    }
    
    /**
     * 归档会话（设置状态为2）
     */
    @Transactional
    public boolean archiveConversation(Integer id, Integer userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(id, userId);
        if (conversation == null) {
            return false;
        }
        
        conversation.setStatus(2);
        conversationRepository.save(conversation);
        return true;
    }
    
    /**
     * 删除会话（设置状态为0）
     */
    @Transactional
    public boolean deleteConversation(Integer id, Integer userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(id, userId);
        if (conversation == null) {
            return false;
        }
        
        conversation.setStatus(0);
        conversationRepository.save(conversation);
        return true;
    }
    
    /**
     * 转换为DTO
     */
    private ConversationDTO convertToDTO(Conversation conversation) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());
        dto.setUserId(conversation.getUserId());
        dto.setTitle(conversation.getTitle());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        dto.setStatus(conversation.getStatus());
        return dto;
    }
} 