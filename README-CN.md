# AI SSE 聊天系统

这是一个基于 Server-Sent Events (SSE) 的全栈 AI 聊天演示项目，后端采用 Spring Boot + MySQL，前端采用 React，支持多会话、消息持久化、AI流式回复、现代化大气 UI。

---

## 功能亮点

- **多会话支持**：可创建多个会话，历史消息持久化存储于 MySQL。
- **AI流式回复**：AI回复内容边生成边展示，体验丝滑。
- **消息去重与优化**：用户消息无重复，AI回复流畅合并。
- **现代化UI**：响应式设计，消息区与输入框无缝贴合，界面大气美观。
- **SSE实时推送**：后端通过SSE实时推送AI回复，前端自动流式渲染。

---

## 技术栈

- **后端**：Java 11, Spring Boot 2.7.x, Spring Web, Spring Data JPA, MySQL, SseEmitter
- **前端**：React 17, Axios, EventSource API, CSS Flexbox

---

## 目录结构

```
.
├── src/
│   └── main/
│       ├── java/com/example/sse/
│       │   ├── controller/         # REST/SSE控制器
│       │   ├── service/            # 业务逻辑（含AI流式、消息持久化）
│       │   ├── model/              # JPA实体
│       │   ├── repository/         # JPA仓库
│       │   └── SseApplication.java # 启动类
│       └── resources/
│           ├── application.properties # 配置
│           └── schema.sql             # 数据库表结构
├── frontend/
│   ├── src/
│   │   ├── components/            # ChatBox等React组件
│   │   ├── App.js
│   │   └── index.js
│   └── package.json
└── pom.xml
```

---

## 快速启动

### 1. 后端

1. 安装 MySQL，创建数据库 `agent`，并导入 `src/main/resources/schema.sql`。
2. 配置 `src/main/resources/application.properties` 数据库账号密码。
3. 启动后端服务：

   ```bash
   mvn spring-boot:run
   # 默认端口 http://localhost:8080
   ```

### 2. 前端

1. 进入 `frontend` 目录，安装依赖：

   ```bash
   cd frontend
   npm install
   ```

2. 启动前端开发服务器：

   ```bash
   npm start
   # 默认端口 http://localhost:3000
   ```

---

## 主要接口说明

- `GET /api/sse/subscribe`：建立SSE连接，推送AI流式消息
- `POST /api/sse/chat`：发送用户消息，自动推送AI回复
- `GET /api/sse/trigger`：触发测试事件
- 其它会话/消息相关接口见后端代码

---

## 前端主要特性

- **消息输入框与消息区无缝贴合**，自适应高度
- **AI回复流式展示**，体验接近ChatGPT
- **发送按钮在AI回复期间自动禁用**，防止打断
- **消息去重**，无重复用户消息
- **响应式设计**，适配PC和移动端

---

## 数据库结构

- `conversations`：会话表，记录会话元信息
- `messages`：消息表，记录每条对话消息（用户/AI）

---

## 体验流程

1. 创建/选择会话，输入消息
2. 消息立即本地显示，AI回复流式推送
3. AI回复期间发送按钮禁用，防止打断
4. 所有消息持久化，可随时切换会话查看历史

---

## 其他说明

- 如需自定义AI模型或API，请修改 `OllamaService.java`
- 前端样式可在 `frontend/src/index.css` 自由调整
- 支持多用户并发、消息实时推送

---

如有问题或建议，欢迎提 issue 或 PR！