package com.example.sse.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.sse.dto.MessageDTO;

@Service
public class SseService {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);
    
    // 默认超时时间设置为1小时
    private static final long DEFAULT_TIMEOUT = 60 * 60 * 1000L;
    
    // 记录连接的客户端
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    // 用于生成唯一ID
    private final AtomicLong counter = new AtomicLong();
    
    // 事件发布器，用于异步处理AI响应
    private final ApplicationEventPublisher eventPublisher;
    
    @Autowired
    public SseService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 创建新的SSE连接
     */
    public SseEmitter createEmitter() {
        String emitterId = String.valueOf(counter.incrementAndGet());
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        
        // 设置超时回调
        emitter.onTimeout(() -> {
            log.info("SSE连接超时: {}", emitterId);
            emitter.complete();
            emitters.remove(emitterId);
        });
        
        // 设置完成回调
        emitter.onCompletion(() -> {
            log.info("SSE连接完成: {}", emitterId);
            emitters.remove(emitterId);
        });
        
        // 设置错误回调
        emitter.onError(ex -> {
            log.error("SSE连接发生错误: {}", emitterId, ex);
            emitter.complete();
            emitters.remove(emitterId);
        });
        
        // 发送连接成功消息
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("连接成功 - ID: " + emitterId));
        } catch (IOException e) {
            log.error("发送连接成功消息失败", e);
            emitter.complete();
            return emitter;
        }
        
        // 将emitter存储在map中
        emitters.put(emitterId, emitter);
        log.info("新的SSE连接已创建: {}, 当前连接数: {}", emitterId, emitters.size());
        
        return emitter;
    }

    /**
     * 向所有客户端发送消息
     */
    public void sendToAll(String eventName, Object data) {
        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                log.info("向客户端 {} 发送消息类型: {}", id, eventName);
            } catch (IOException e) {
                log.error("向客户端 {} 发送消息失败", id, e);
                emitter.complete();
                emitters.remove(id);
            }
        });
    }
    
    /**
     * 向特定客户端发送消息
     */
    public void sendToClient(String clientId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                log.info("向客户端 {} 发送消息类型: {}", clientId, eventName);
            } catch (IOException e) {
                log.error("向客户端 {} 发送消息失败", clientId, e);
                emitter.complete();
                emitters.remove(clientId);
            }
        } else {
            log.warn("客户端 {} 不存在或已断开连接", clientId);
        }
    }
    
    /**
     * 处理用户发送的消息
     * 此方法与MessageService解耦，作为初始处理入口
     * @param message 用户消息内容
     */
    public void processUserMessage(String message) {
        log.info("收到用户消息: {}", message);
        
        // 发布消息处理事件，由专门的服务处理
        // 临时实现：直接回显用户消息
        String response = "服务器已收到您的消息: " + message;
        
        // 向所有客户端发送响应
        sendToAll("用户消息响应", response);
    }
    
    /**
     * 发送新消息通知
     */
    public void sendMessageNotification(MessageDTO messageDTO) {
        sendToAll("新消息", messageDTO);
    }
    
    /**
     * 获取当前活跃连接数
     */
    public int getActiveConnectionsCount() {
        return emitters.size();
    }
} 