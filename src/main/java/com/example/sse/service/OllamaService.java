package com.example.sse.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class OllamaService {

    private final WebClient webClient;
    private static final String OLLAMA_API_URL = "http://localhost:11434";
    
    // 默认模型名称，可以根据本地安装的模型进行调整
    private static final String DEFAULT_MODEL = "llama3.2";

    public OllamaService() {
        this.webClient = WebClient.builder()
                .baseUrl(OLLAMA_API_URL)
                .build();
    }

    /**
     * 生成非流式响应
     * @param userMessage 用户消息
     * @return AI响应内容
     */
    public String generateResponse(String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", DEFAULT_MODEL);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);

        try {
            Map<String, Object> response = webClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("message")) {
                Map<String, String> messageObj = (Map<String, String>) response.get("message");
                return messageObj.get("content");
            } else {
                return "无法从Ollama获取响应";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "调用Ollama API时发生错误: " + e.getMessage();
        }
    }

    /**
     * 生成流式响应，支持实时显示AI生成过程
     * @param userMessage 用户消息
     * @param onChunk 处理每个响应块的回调
     * @param onComplete 完成时的回调
     * @param onError 发生错误时的回调
     */
    public void generateResponseStream(String userMessage, Consumer<String> onChunk, 
                                      Runnable onComplete, Consumer<Throwable> onError) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", DEFAULT_MODEL);
        requestBody.put("messages", messages);
        requestBody.put("stream", true);

        System.out.println(requestBody);

        Flux<Map> responseFlux = webClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(Map.class);

        responseFlux.subscribe(
                chunk -> {
                    if (chunk.containsKey("message")) {
                        Map<String, String> messageObj = (Map<String, String>) chunk.get("message");
                        String content = messageObj.get("content");
                        if (content != null && !content.isEmpty()) {
                            onChunk.accept(content);
                        }
                    }
                },
                onError,
                onComplete
        );
    }

    /**
     * 为会话生成完整响应，流式传输给客户端
     * @param userMessage 用户消息
     * @param conversationId 会话ID
     * @param messageService 消息服务，用于保存回复
     * @param sseService SSE服务，用于发送事件
     */
    public void generateConversationResponse(String userMessage, Integer conversationId, 
                                           MessageService messageService, SseService sseService) {
        // 创建临时AI消息对象
        StringBuilder fullResponse = new StringBuilder();
        
        // 使用流式API获取响应
        generateResponseStream(
            userMessage,
            // 每收到一个响应块
            chunk -> {
                fullResponse.append(chunk);
                // 发送流式更新事件
                sseService.sendToAll("AI响应流", Map.of(
                    "conversationId", conversationId,
                    "content", chunk,
                    "senderType", 2, // AI类型
                    "isPartial", true
                ));
            },
            // 完成时保存完整消息
            () -> {
                String completeResponse = fullResponse.toString();
                // 保存完整的AI响应
                if (!completeResponse.isEmpty()) {
                    messageService.addAiResponse(conversationId, completeResponse);
                }
                
                // 发送AI响应结束事件，通知前端完成消息流
                sseService.sendToAll("AI响应结束", Map.of(
                    "conversationId", conversationId,
                    "complete", true
                ));
            },
            // 错误处理
            error -> {
                // 发送错误通知
                sseService.sendToAll("错误", Map.of(
                    "conversationId", conversationId,
                    "error", "AI响应生成失败: " + error.getMessage()
                ));
                // 保存一个错误消息
                messageService.addAiResponse(conversationId, "抱歉，生成回复时出现错误");
            }
        );
    }

    /**
     * 生成简短的自我介绍
     */
    public String generateIntroductionResponse() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", DEFAULT_MODEL);
        requestBody.put("prompt", "你好，请用两句话介绍一下自己");
        requestBody.put("stream", false);
        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.7);
        options.put("max_tokens", 100);
        requestBody.put("options", options);
    
        try {
            Map<String, Object> response = webClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
    
            if (response != null && response.containsKey("response")) {
                return (String) response.get("response");
            } else {
                return "我是您的AI助手，随时为您提供帮助。";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "我是您的AI助手，随时为您提供帮助。";
        }
    }

    /**
     * 生成动态响应
     */
    public String generateDynamicResponse(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", DEFAULT_MODEL);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);
        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.7);
        options.put("max_tokens", 2000);
        requestBody.put("options", options);
    
        try {
            Map<String, Object> response = webClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
    
            if (response != null && response.containsKey("response")) {
                return (String) response.get("response");
            } else {
                return "无法从Ollama获取响应";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "调用Ollama API时发生错误: " + e.getMessage();
        }
    }
}