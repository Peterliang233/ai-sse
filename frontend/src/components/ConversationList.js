import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_URL = 'http://localhost:8080/api';

// 删除有问题的ESLint注释
/* eslint-disable */

function ConversationList({ onSelectConversation, selectedConversationId }) {
  const [conversations, setConversations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [newTitle, setNewTitle] = useState('');
  const [showNewForm, setShowNewForm] = useState(false);
  // 假设用户ID为1，实际项目中应从认证系统获取
  const userId = 1;

  // 加载会话列表
  const loadConversations = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get(`${API_URL}/conversations?userId=${userId}`);
      setConversations(response.data);
      
      // 如果有会话但没有选中任何会话，自动选择第一个
      if (response.data.length > 0 && !selectedConversationId) {
        onSelectConversation(response.data[0]);
      }
    } catch (error) {
      console.error('加载会话列表失败:', error);
      setError('无法加载会话列表，请检查网络连接或稍后再试');
    } finally {
      setLoading(false);
    }
  };

  // 创建新会话
  const createConversation = async (e) => {
    e.preventDefault();
    if (!newTitle.trim()) return;

    setLoading(true);
    setError(null);
    try {
      const response = await axios.post(`${API_URL}/conversations`, {
        userId,
        title: newTitle.trim()
      });
      
      // 将新会话添加到列表最前面
      const newConversation = response.data;
      setConversations([newConversation, ...conversations]);
      setNewTitle('');
      setShowNewForm(false);
      
      // 自动选择新创建的会话
      onSelectConversation(newConversation);
    } catch (error) {
      console.error('创建会话失败:', error);
      setError('无法创建新会话，请稍后再试');
    } finally {
      setLoading(false);
    }
  };

  // 删除会话
  const deleteConversation = async (id, e) => {
    e.stopPropagation(); // 防止触发选择事件
    
    if (!window.confirm('确定要删除这个会话吗？删除后将无法恢复。')) return;
    
    try {
      await axios.delete(`${API_URL}/conversations/${id}?userId=${userId}`);
      
      // 从列表中移除被删除的会话
      setConversations(conversations.filter(conv => conv.id !== id));
      
      // 如果删除的是当前选中的会话，清除选择或选中第一个会话
      if (selectedConversationId === id) {
        const remainingConversations = conversations.filter(conv => conv.id !== id);
        if (remainingConversations.length > 0) {
          onSelectConversation(remainingConversations[0]);
        } else {
          onSelectConversation(null);
        }
      }
    } catch (error) {
      console.error('删除会话失败:', error);
      alert('删除会话失败，请稍后再试');
    }
  };

  // 刷新会话列表
  const refreshConversations = () => {
    loadConversations();
  };

  // 首次加载时获取会话列表
  useEffect(() => {
    loadConversations();
  }, []);

  return (
    <div className="conversation-list-container">
      <div className="conversation-list-header">
        <h2>我的会话</h2>
        <div className="conversation-actions">
          <button 
            className="refresh-button"
            onClick={refreshConversations}
            disabled={loading}
            title="刷新会话列表"
          >
            ↻
          </button>
          <button 
            className="new-conversation-button"
            onClick={() => setShowNewForm(!showNewForm)}
            disabled={loading}
          >
            {showNewForm ? '取消' : '新建会话'}
          </button>
        </div>
      </div>
      
      {showNewForm && (
        <form onSubmit={createConversation} className="new-conversation-form">
          <input
            type="text"
            value={newTitle}
            onChange={(e) => setNewTitle(e.target.value)}
            placeholder="输入会话标题..."
            disabled={loading}
            autoFocus
          />
          <button 
            type="submit" 
            disabled={loading || !newTitle.trim()}
          >
            创建
          </button>
        </form>
      )}
      
      {error && (
        <div className="error-message">{error}</div>
      )}
      
      {loading && conversations.length === 0 ? (
        <p className="loading-message">加载中...</p>
      ) : conversations.length === 0 ? (
        <p className="empty-message">暂无会话，点击"新建会话"开始聊天</p>
      ) : (
        <ul className="conversation-list">
          {conversations.map(conversation => (
            <li 
              key={conversation.id} 
              className={`conversation-item ${selectedConversationId === conversation.id ? 'selected' : ''}`}
              onClick={() => onSelectConversation(conversation)}
            >
              <div className="conversation-title">{conversation.title}</div>
              <div className="conversation-meta">
                {conversation.messageCount ? `${conversation.messageCount}条消息` : '无消息'}
                <span className="conversation-time">
                  {new Date(conversation.updatedAt).toLocaleDateString()}
                </span>
              </div>
              {conversation.lastMessage && (
                <div className="conversation-preview">{conversation.lastMessage}</div>
              )}
              <button 
                className="delete-button"
                onClick={(e) => deleteConversation(conversation.id, e)}
                title="删除会话"
              >
                删除
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default ConversationList; 