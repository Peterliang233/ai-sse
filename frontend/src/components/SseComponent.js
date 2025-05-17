import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';

const API_URL = 'http://localhost:8080/api/sse';

function SseComponent() {
  const [connected, setConnected] = useState(false);
  const [messages, setMessages] = useState([]);
  const [connectionId, setConnectionId] = useState(null);
  const [userInput, setUserInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const eventSourceRef = useRef(null);
  const messageListRef = useRef(null);

  // 连接到SSE
  const connectToSSE = () => {
    // 如果已经有连接，先关闭
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }

    setIsLoading(true);
    
    // 创建新的EventSource对象
    const eventSource = new EventSource(`${API_URL}/subscribe`);
    eventSourceRef.current = eventSource;

    // 连接打开事件
    eventSource.onopen = () => {
      console.log('SSE连接已建立');
      setConnected(true);
      setIsLoading(false);
    };

    // 连接成功事件
    eventSource.addEventListener('connect', (event) => {
      const data = event.data;
      console.log('连接事件:', data);
      
      // 从连接成功消息中提取ID
      const idMatch = data.match(/ID: (\d+)/);
      if (idMatch && idMatch[1]) {
        setConnectionId(idMatch[1]);
      }
      
      addMessage('连接', data);
      setIsLoading(false);
    });

    // 测试事件
    eventSource.addEventListener('测试事件', (event) => {
      console.log('收到测试事件:', event.data);
      addMessage('测试事件', event.data);
    });

    // 接收用户消息响应事件
    eventSource.addEventListener('用户消息响应', (event) => {
      console.log('收到用户消息响应:', event.data);
      addMessage('服务器响应', event.data);
    });

    // 错误处理
    eventSource.onerror = (error) => {
      console.error('SSE连接错误:', error);
      setConnected(false);
      setIsLoading(false);
      eventSource.close();
      
      // 添加错误消息
      addMessage('错误', '连接已关闭或发生错误');
    };
  };

  // 断开SSE连接
  const disconnectSSE = () => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
      setConnected(false);
      setConnectionId(null);
      addMessage('断开连接', '已主动断开连接');
    }
  };

  // 触发后端事件
  const triggerEvent = async () => {
    if (!connected) return;
    
    setIsLoading(true);
    try {
      await axios.get(`${API_URL}/trigger`);
      console.log('事件触发请求已发送');
      setIsLoading(false);
    } catch (error) {
      console.error('触发事件错误:', error);
      addMessage('错误', `触发事件失败: ${error.message}`);
      setIsLoading(false);
    }
  };

  // 发送用户输入的消息
  const sendUserMessage = async () => {
    if (!userInput.trim() || !connected || isLoading) return;
    
    setIsLoading(true);
    try {
      // 记录发送的消息
      addMessage('用户消息', userInput);
      
      // 发送到后端
      await axios.post(`${API_URL}/sendMessage`, { message: userInput });
      
      // 清空输入框
      setUserInput('');
      setIsLoading(false);
    } catch (error) {
      console.error('发送消息错误:', error);
      addMessage('错误', `发送消息失败: ${error.message}`);
      setIsLoading(false);
    }
  };

  // 添加消息到列表
  const addMessage = (type, content) => {
    const newMessage = {
      id: Date.now(),
      type,
      content,
      timestamp: new Date().toLocaleTimeString()
    };
    
    setMessages(prevMessages => [newMessage, ...prevMessages]);
  };

  // 清空消息列表
  const clearMessages = () => {
    setMessages([]);
  };

  // 处理按Enter键发送消息
  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendUserMessage();
    }
  };
  
  // 自动滚动到最新消息
  useEffect(() => {
    if (messageListRef.current) {
      messageListRef.current.scrollTop = 0;
    }
  }, [messages]);

  // 组件卸载时清理资源
  useEffect(() => {
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
    };
  }, []);

  return (
    <div>
      <div className="card">
        <h2>连接状态</h2>
        <div className="status-container">
          <p>
            状态: 
            <span className={`connection-status ${connected ? 'connected' : 'disconnected'}`}>
              {connected ? '已连接' : '未连接'}
              {isLoading && '...'}
            </span>
          </p>
          {connectionId && <p>连接 ID: <strong>{connectionId}</strong></p>}
        </div>
        
        <div className="button-group">
          {!connected ? (
            <button 
              onClick={connectToSSE} 
              disabled={isLoading}
              className="primary-button"
            >
              {isLoading ? '连接中...' : '连接到SSE'}
            </button>
          ) : (
            <button 
              onClick={disconnectSSE}
              className="disconnect-button"
            >
              断开连接
            </button>
          )}
          
          <button 
            onClick={triggerEvent} 
            disabled={!connected || isLoading}
            className="event-button"
          >
            {isLoading ? '触发中...' : '触发服务器事件'}
          </button>
          
          <button 
            onClick={clearMessages}
            className="clear-button"
          >
            清空消息
          </button>
        </div>
      </div>

      {/* 用户输入区域 */}
      <div className="card">
        <h2>发送消息</h2>
        <div className="input-container">
          <input
            type="text"
            value={userInput}
            onChange={(e) => setUserInput(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="输入要发送的消息..."
            disabled={!connected || isLoading}
            className="message-input"
          />
          <button 
            onClick={sendUserMessage} 
            disabled={!connected || !userInput.trim() || isLoading}
            className="send-button"
          >
            {isLoading ? '发送中...' : '发送'}
          </button>
        </div>
        <p className="input-hint">按Enter键快速发送</p>
      </div>

      <div className="card">
        <h2>事件消息 <span className="message-count">({messages.length})</span></h2>
        
        {messages.length === 0 ? (
          <p className="empty-message">暂无消息</p>
        ) : (
          <div className="message-list" ref={messageListRef}>
            {messages.map((message) => (
              <div key={message.id} className="message-item" data-type={message.type}>
                <strong>{message.type}:</strong> {message.content}
                <div className="message-time">{message.timestamp}</div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default SseComponent; 