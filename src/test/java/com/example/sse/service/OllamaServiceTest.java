package com.example.sse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OllamaServiceTest {

    @Mock
    private WebClient webClient;

    private OllamaService ollamaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ollamaService = new OllamaService();
    }

    @Test
    void testGenerateResponseError() {
        // Mock the WebClient behavior to throw an exception
        when(webClient.post()).thenThrow(new RuntimeException("API error"));

        String userMessage = "Hello, Ollama!";

        // Call the method
        String actualResponse = ollamaService.generateResponse(userMessage);

        // Verify the response
        assertTrue(actualResponse.contains("调用Ollama API时发生错误"));
    }

    @Test
    void testGenerateDynamicResponse() {
        // Mock the WebClient behavior to return a specific response    
        String prompt = "介绍一下你自己吧";
    
        // Call the method
        String actualResponse = ollamaService.generateDynamicResponse(prompt);
    
       System.out.println("输出结果：" + actualResponse);
    }
}