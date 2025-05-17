import React, { useState, useEffect, useRef, useLayoutEffect } from 'react';
import axios from 'axios';

const API_URL = 'http://localhost:8080/api';

// 删除有问题的ESLint注释
/* eslint-disable */

function ChatBox({ conversation, connected, onSendMessage }) {
  const [messages, setMessages] = useState([]);
  const [userInput, setUserInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [sending, setSending] = useState(false);
  const [aiResponding, setAiResponding] = useState(false);
  const messageListRef = useRef(null);
  const inputRef = useRef(null);
  const streamingMessageRef = useRef(null);
  // 假设用户ID为1，实际项目中应从认证系统获取
  const userId = 1;

  // 加载会话消息
  const loadMessages = async () => {
    if (!conversation) return;
    
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get(
        `${API_URL}/messages?conversationId=${conversation.id}&userId=${userId}`
      );
      setMessages(response.data || []);
    } catch (error) {
      console.error('加载消息失败:', error);
      setError('无法加载消息，请检查网络连接或稍后再试');
    } finally {
      setLoading(false);
    }
  };

  // 发送消息
  const sendMessage = async (e) => {
    e?.preventDefault();
    if (!userInput.trim() || !connected || !conversation || sending) return;
    
    setSending(true);
    setError(null);
    
    // 先关闭任何现有的流式消息
    setMessages(prev => 
      prev.map(m => 
        m.streaming ? { ...m, streaming: false } : m
      )
    );
    
    // 重置流式消息引用
    streamingMessageRef.current = null;
    
    // 先添加用户消息到界面（乐观UI更新）
    const userMessage = {
      id: 'temp-' + Date.now(),
      content: userInput.trim(),
      senderType: 1, // 用户类型
      createdAt: new Date().toISOString(),
      conversationId: conversation.id,
      temporary: true // 标记为临时消息
    };
    
    setMessages(prev => [...prev, userMessage]);
    
    // 创建AI临时消息占位符
    const aiTempMessage = {
      id: 'ai-temp-' + Date.now(),
      content: '', // 初始为空
      senderType: 2, // AI类型
      createdAt: new Date().toISOString(),
      conversationId: conversation.id,
      temporary: true, // 标记为临时消息
      streaming: true // 标记为正在流式接收内容
    };
    
    // 添加AI临时消息到列表
    setMessages(prev => [...prev, aiTempMessage]);
    
    // 保存AI临时消息的引用，用于后续更新
    streamingMessageRef.current = aiTempMessage;
    
    // 清空输入框
    const sentMessage = userInput.trim();
    setUserInput('');
    
    try {
      // 发送到聊天API - 这里只需要发送消息，不需要处理响应
      // SSE会自动推送消息更新
      await axios.post(`${API_URL}/sse/chat`, {
        conversationId: conversation.id,
        userId,
        content: sentMessage
      });
      
      // 替换用户临时消息为正式消息
      setMessages(prev => 
        prev.map(m => 
          (m.id === userMessage.id) 
            ? {...m, temporary: false}
            : m
        )
      );
      
      // 标记为AI开始响应
      setAiResponding(true);
      
      // 调用回调
      if (onSendMessage) {
        onSendMessage(sentMessage);
      }
    } catch (error) {
      console.error('发送消息失败:', error);
      setError('发送消息失败，请稍后再试');
      setAiResponding(false);
      
      // 将临时消息标记为发送失败
      setMessages(prev => {
        // 移除AI临时消息
        const filtered = prev.filter(m => m.id !== aiTempMessage.id);
        // 将用户消息标记为失败
        return filtered.map(m => m.id === userMessage.id ? {...m, failed: true} : m);
      });
    } finally {
      setSending(false);
    }
  };

  // 重试发送失败的消息
  const retryMessage = (message) => {
    // 移除失败消息
    setMessages(prev => prev.filter(m => m.id !== message.id));
    
    // 设置到输入框
    setUserInput(message.content);
    
    // 聚焦到输入框
    if (inputRef.current) {
      inputRef.current.focus();
    }
  };

  // 处理按Enter键发送消息
  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };
  
  // 当会话ID变化时加载消息
  useEffect(() => {
    if (conversation?.id) {
      loadMessages();
      
      // 当会话改变时，聚焦到输入框
      setTimeout(() => {
        if (inputRef.current) {
          inputRef.current.focus();
        }
      }, 100);
      
      // 重置流式消息引用
      streamingMessageRef.current = null;
    }
  }, [conversation?.id]);

  // 监听SSE消息更新
  useEffect(() => {
    if (!connected) return;
    
    const eventSource = new EventSource(`${API_URL}/sse/subscribe`);
    
    // 连接事件
    eventSource.onopen = () => {
      console.log('SSE连接已建立');
    };
    
    // 错误处理
    eventSource.onerror = (error) => {
      console.error('SSE连接错误:', error);
    };
    
    // 处理AI响应流（实时更新临时消息内容）
    eventSource.addEventListener('AI响应流', (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.conversationId === conversation?.id) {
          const chunk = data.content;
          
          // 标记AI正在响应
          setAiResponding(true);
          
          // 更新流式消息内容 - 将消息块聚合到一起
          setMessages(prev => {
            // 查找当前正在流式接收的消息
            const currentAiMessage = prev.find(m => m.streaming === true);
            
            if (currentAiMessage) {
              // 更新现有消息的内容，将新块附加到现有内容
              return prev.map(m => 
                m.id === currentAiMessage.id 
                  ? { ...m, content: m.content + chunk } 
                  : m
              );
            } else if (streamingMessageRef.current) {
              // 如果列表中没有正在流式接收的消息，但有引用，则用它创建新消息
              const newStreamingMessage = {
                ...streamingMessageRef.current,
                content: chunk,
              };
              
              // 确保过滤掉任何可能存在的其他流式消息
              const filteredMessages = prev.filter(m => !m.streaming);
              return [...filteredMessages, newStreamingMessage];
            }
            return prev;
          });
        }
      } catch (error) {
        console.error('处理流式SSE消息失败:', error);
      }
    });
    
    // 处理AI响应消息（完整消息，替换临时消息）
    eventSource.addEventListener('AI响应', (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.conversationId === conversation?.id) {
          // 不要移除流式消息，而是将流式消息更新为最终消息
          setMessages(prev => {
            const streamingMessage = prev.find(m => m.streaming === true);
            
            // 如果存在流式消息，将其替换为最终消息
            if (streamingMessage) {
              return prev.map(m => 
                m.streaming ? { 
                  ...data, 
                  id: streamingMessage.id, // 保持ID以防止消息重复
                  streaming: false 
                } : m
              );
            } else {
              // 检查消息是否已存在
              if (!prev.find(m => 
                  m.id === data.id || 
                  (m.content === data.content && m.senderType === data.senderType && !m.temporary)
                )) {
                return [...prev, { ...data, streaming: false }];
              }
              return prev;
            }
          });
          
          // 重置流式消息引用
          streamingMessageRef.current = null;
          
          // 标记AI响应完成
          setAiResponding(false);
        }
      } catch (error) {
        console.error('处理SSE消息失败:', error);
        setAiResponding(false);
      }
    });
    
    // 新增事件：处理AI响应结束信号
    eventSource.addEventListener('AI响应结束', (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.conversationId === conversation?.id) {
          // 将所有流式消息标记为非流式
          setMessages(prev => 
            prev.map(m => 
              m.streaming ? { ...m, streaming: false } : m
            )
          );
          
          // 重置流式消息引用
          streamingMessageRef.current = null;
          
          // 标记AI响应完成
          setAiResponding(false);
        }
      } catch (error) {
        console.error('处理AI响应结束事件失败:', error);
        setAiResponding(false);
      }
    });
    
    // 处理新消息事件
    eventSource.addEventListener('新消息', (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.conversationId === conversation?.id) {
          // 检查消息是否已存在，避免重复
          setMessages(prev => {
            // 检查此消息是否已存在（避免重复）
            const messageExists = prev.some(m => 
              m.id === data.id || 
              (m.senderType === data.senderType && 
               m.content === data.content && 
               !m.temporary && 
               Math.abs(new Date(m.createdAt) - new Date(data.createdAt)) < 1000) // 创建时间相近
            );
            
            if (messageExists) {
              return prev;
            }
            
            // 如果是用户消息，检查是否有相同内容的临时用户消息
            if (data.senderType === 1) {
              const tempUserMessage = prev.find(m => 
                m.temporary && 
                m.senderType === 1 && 
                m.content === data.content
              );
              
              if (tempUserMessage) {
                // 替换临时消息
                return prev.map(m => 
                  m.id === tempUserMessage.id ? data : m
                );
              }
            }
            
            // 否则添加为新消息
            return [...prev, data];
          });
        }
      } catch (error) {
        console.error('处理SSE消息失败:', error);
      }
    });
    
    // 处理错误消息
    eventSource.addEventListener('错误', (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.conversationId === conversation?.id) {
          setError(data.error || '发生错误');
          
          // 移除正在流式接收的消息
          setMessages(prev => prev.filter(m => !m.streaming));
          
          // 重置流式消息引用
          streamingMessageRef.current = null;
          
          // 标记AI响应完成
          setAiResponding(false);
        }
      } catch (error) {
        console.error('处理错误消息失败:', error);
        setAiResponding(false);
      }
    });
    
    return () => {
      eventSource.close();
    };
  }, [connected, conversation?.id]);
  
  // 自动滚动到最新消息（多次setTimeout兜底，确保DOM已更新）
  useLayoutEffect(() => {
    if (messageListRef.current) {
      for (let i = 0; i < 3; i++) {
        setTimeout(() => {
          if (messageListRef.current) {
            messageListRef.current.scrollTop = messageListRef.current.scrollHeight;
          }
        }, i * 50);
      }
    }
  }, [messages]);

  if (!conversation) {
    return (
      <div className="chat-box empty-state">
        <p>请选择或创建一个会话开始聊天</p>
      </div>
    );
  }

  return (
    <div className="chat-box">
      <div className="chat-header">
        <h2>{conversation.title}</h2>
        <div className="chat-status">
          <span className={`connection-status ${connected ? 'connected' : 'disconnected'}`}>
            {connected ? '已连接' : '未连接'}
          </span>
        </div>
      </div>
      
      <div className="message-container" ref={messageListRef}>
        {error && (
          <div className="error-message">{error}</div>
        )}
        
        {loading && messages.length === 0 ? (
          <p className="loading-message">加载消息中...</p>
        ) : messages.length === 0 ? (
          <p className="empty-message">暂无消息，发送消息开始对话</p>
        ) : (
          <div className="message-list">
            {messages.map((message) => (
              <div 
                key={message.id} 
                className={`message-bubble ${message.senderType === 1 ? 'user-message' : 'ai-message'} ${message.failed ? 'failed' : ''} ${message.temporary ? 'temporary' : ''} ${message.streaming ? 'streaming' : ''}`}
              >
                <div className="message-content">
                  {message.content || (message.streaming ? '生成中...' : '')}
                  {message.streaming && <span className="cursor-blink">|</span>}
                </div>
                <div className="message-time">
                  {new Date(message.createdAt).toLocaleString()}
                </div>
                {message.failed && (
                  <button 
                    className="retry-button"
                    onClick={() => retryMessage(message)}
                    title="重试发送此消息"
                  >
                    重试
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
      
      <form className="message-input-container" onSubmit={sendMessage}>
        <input
          type="text"
          value={userInput}
          onChange={(e) => setUserInput(e.target.value)}
          onKeyPress={handleKeyPress}
          placeholder="输入消息..."
          disabled={!connected || loading || sending || aiResponding}
          className="message-input"
          ref={inputRef}
        />
        <button 
          type="submit"
          disabled={!connected || !userInput.trim() || sending || aiResponding}
          className="send-button"
        >
          {sending ? '发送中...' : (aiResponding ? 'AI响应中...' : '发送')}
        </button>
      </form>
    </div>
  );
}

export default ChatBox; 