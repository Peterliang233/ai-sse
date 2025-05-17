package com.example.sse.repository;

import com.example.sse.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Integer> {
    
    // Find conversations by user ID and active status
    List<Conversation> findByUserIdAndStatusOrderByUpdatedAtDesc(Integer userId, Integer status);
    
    // Find conversations by ID and user ID (for security)
    Conversation findByIdAndUserId(Integer id, Integer userId);
} 