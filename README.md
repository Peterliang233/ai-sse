**<a href="./README-CN.md">中文版本</a>**
---

# AI SSE Chat System (English)

This is a full-stack AI chat demo project based on Server-Sent Events (SSE). Backend uses Spring Boot + MySQL, frontend uses React. Features include multi-session, message persistence, AI streaming reply, and a modern, responsive UI.

---

## Features

- **Multi-session support**: Create multiple chat sessions, all messages are persisted in MySQL.
- **AI streaming reply**: AI replies are streamed and displayed in real time.
- **Message deduplication**: No duplicate user messages, AI replies are smoothly merged.
- **Modern UI**: Responsive design, seamless message area and input box, beautiful and professional look.
- **SSE real-time push**: Backend pushes AI replies via SSE, frontend renders them in real time.

---

## Tech Stack

- **Backend**: Java 11, Spring Boot 2.7.x, Spring Web, Spring Data JPA, MySQL, SseEmitter
- **Frontend**: React 17, Axios, EventSource API, CSS Flexbox

---

## Project Structure

```
.
├── src/
│   └── main/
│       ├── java/com/example/sse/
│       │   ├── controller/         # REST/SSE controllers
│       │   ├── service/            # Business logic (AI streaming, persistence)
│       │   ├── model/              # JPA entities
│       │   ├── repository/         # JPA repositories
│       │   └── SseApplication.java # Main class
│       └── resources/
│           ├── application.properties # Config
│           └── schema.sql             # DB schema
├── frontend/
│   ├── src/
│   │   ├── components/            # React components (ChatBox, etc.)
│   │   ├── App.js
│   │   └── index.js
│   └── package.json
└── pom.xml
```

---

## Quick Start

### 1. Backend

1. Install MySQL, create database `agent`, and import `src/main/resources/schema.sql`.
2. Configure DB username/password in `src/main/resources/application.properties`.
3. Start backend:

   ```bash
   mvn spring-boot:run
   # Default: http://localhost:8080
   ```

### 2. Frontend

1. Go to `frontend` and install dependencies:

   ```bash
   cd frontend
   npm install
   ```

2. Start frontend dev server:

   ```bash
   npm start
   # Default: http://localhost:3000
   ```

---

## Main API Endpoints

- `GET /api/sse/subscribe`: Establish SSE connection, stream AI messages
- `POST /api/sse/chat`: Send user message, receive AI reply via SSE
- `GET /api/sse/trigger`: Trigger a test event
- See backend code for more session/message APIs

---

## Frontend Highlights

- **Seamless message area and input box**, adaptive height
- **AI reply streaming display**, ChatGPT-like experience
- **Send button auto-disabled during AI reply**, prevents interruption
- **Message deduplication**, no duplicate user messages
- **Responsive design**, works on PC and mobile

---

## Database Schema

- `conversations`: Session table, stores session meta info
- `messages`: Message table, stores each chat message (user/AI)

---

## Typical Flow

1. Create/select a session, input message
2. Message is shown instantly, AI reply is streamed
3. Send button is disabled during AI reply
4. All messages are persisted, you can switch sessions and view history anytime

---

## Notes

- To customize AI model or API, edit `OllamaService.java`
- Frontend styles can be freely adjusted in `frontend/src/index.css`
- Supports multi-user concurrency and real-time message push

---

For questions or suggestions, feel free to open an issue or PR!