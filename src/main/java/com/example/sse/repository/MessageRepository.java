package com.example.sse.repository;

import com.example.sse.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {
    
    // Find messages by conversation ID
    List<Message> findByConversationIdOrderByCreatedAtAsc(Integer conversationId);
    
    // Count messages in a conversation
    Long countByConversationId(Integer conversationId);
} 