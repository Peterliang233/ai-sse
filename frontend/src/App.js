import React, { useState, useEffect } from 'react';
import ConversationList from './components/ConversationList';
import ChatBox from './components/ChatBox';

// 背景粒子组件
const AnimatedBackground = () => {
  useEffect(() => {
    // 创建随机漂浮粒子
    const createParticles = () => {
      const background = document.querySelector('.animated-background');
      if (!background) return;
      
      // 清除现有粒子
      const existingParticles = document.querySelectorAll('.particle');
      existingParticles.forEach(p => p.remove());
      
      // 创建新粒子
      const particleCount = 20;
      for (let i = 0; i < particleCount; i++) {
        const particle = document.createElement('div');
        particle.classList.add('particle');
        
        // 随机大小
        const size = Math.random() * 8 + 2;
        particle.style.width = `${size}px`;
        particle.style.height = `${size}px`;
        
        // 随机位置
        particle.style.left = `${Math.random() * 100}%`;
        particle.style.bottom = `-${size}px`;
        
        // 随机动画延迟和持续时间
        const duration = Math.random() * 10 + 10;
        const delay = Math.random() * 5;
        particle.style.animationDuration = `${duration}s`;
        particle.style.animationDelay = `${delay}s`;
        
        // 随机透明度
        particle.style.opacity = Math.random() * 0.6 + 0.2;
        
        background.appendChild(particle);
      }
    };
    
    // 初始创建
    createParticles();
    
    // 每10秒刷新粒子
    const intervalId = setInterval(createParticles, 10000);
    
    return () => clearInterval(intervalId);
  }, []);
  
  return <div className="animated-background"></div>;
};

function App() {
  const [connected, setConnected] = useState(true); // 默认假设已连接
  const [selectedConversation, setSelectedConversation] = useState(null);

  // 处理选择会话事件
  const handleSelectConversation = (conversation) => {
    setSelectedConversation(conversation);
  };

  // 处理发送消息成功后的回调
  const handleSendMessage = (message) => {
    // 可以在这里添加额外的逻辑
    console.log('消息已发送:', message);
  };

  return (
    <>
      <AnimatedBackground />
      <div className="app-container">
        <h1 className="app-title">
          <span>AI智能助手</span>
        </h1>
        <div className="chat-layout">
          <ConversationList 
            onSelectConversation={handleSelectConversation}
            selectedConversationId={selectedConversation?.id}
          />
          <ChatBox 
            conversation={selectedConversation}
            connected={connected}
            onSendMessage={handleSendMessage}
          />
        </div>
      </div>
    </>
  );
}

export default App; 