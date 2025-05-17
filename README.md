# Server-Sent Events (SSE) 演示项目

这是一个演示服务器发送事件(SSE)技术的全栈应用实例，使用Spring Boot实现后端，React实现前端UI。

## 项目概述

本项目展示了如何使用SSE(Server-Sent Events)创建从服务器到客户端的实时单向通信通道。主要功能包括：

- 建立和管理SSE连接
- 从服务器推送消息到所有连接的客户端
- 客户端发送消息到服务器并接收响应
- 实时显示推送的事件和消息

## 技术栈

### 后端
- Java 11
- Spring Boot 2.7.14
- Spring Web
- Spring WebFlux
- SseEmitter

### 前端
- React 17.0.2
- Axios
- EventSource API

## 项目结构

```
/
├── src/                          # 后端源代码
│   └── main/
│       ├── java/
│       │   └── com/example/sse/
│       │       ├── config/       # CORS配置
│       │       ├── controller/   # REST控制器
│       │       ├── service/      # 业务逻辑
│       │       └── SseApplication.java
│       └── resources/            # 后端配置文件
├── frontend/                     # 前端React应用
│   ├── public/
│   ├── src/
│   │   ├── components/           # React组件
│   │   │   └── SseComponent.js   # SSE客户端组件
│   │   ├── App.js
│   │   └── index.js
│   └── package.json
└── pom.xml                       # Maven配置
```

## 功能说明

### 后端API

- `GET /api/sse/subscribe`: 建立SSE连接，返回SseEmitter
- `GET /api/sse/trigger`: 触发服务器向所有客户端发送测试事件
- `POST /api/sse/sendMessage`: 接收客户端消息，处理后发送响应到所有连接的客户端

### 前端功能

- 建立/断开SSE连接
- 触发服务器事件
- 发送消息到服务器
- 实时显示接收到的服务器事件和消息

## 运行指南

### 后端启动

1. 确保安装了Java 11和Maven
2. 项目根目录下执行：
   ```
   mvn spring-boot:run
   ```
3. 后端服务将在 http://localhost:8080 启动

### 前端启动

1. 确保已安装Node.js和npm
2. 进入frontend目录：
   ```
   cd frontend
   ```
3. 安装依赖：
   ```
   npm install
   ```
4. 启动前端开发服务器：
   ```
   npm start
   ```
5. 浏览器将自动打开 http://localhost:3000

## SSE工作原理

Server-Sent Events (SSE) 是一种HTTP连接上的单向通信技术，允许服务器主动向客户端推送数据。特点：

- 基于标准HTTP协议
- 自动重连
- 单向通信（服务器到客户端）
- 相比WebSocket更轻量，适合单向数据推送场景

在本项目中，服务器使用`SseEmitter`管理连接并发送事件，客户端使用`EventSource`接收事件。

## 示例流程

1. 客户端通过前端UI连接到SSE
2. 服务器为连接分配ID并确认连接
3. 客户端可以点击"触发服务器事件"按钮或发送消息
4. 服务器将事件或消息响应推送到所有连接的客户端
5. 客户端UI实时显示接收到的事件
