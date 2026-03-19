# test-chat-app

A Gradle Groovy DSL monorepo containing a real-time chat application built with Spring Boot 3.5 and Kotlin 2.

## Modules

| Module | Description | Port |
|---|---|---|
| `test-mono-chat-api` | Spring Boot REST & WebSocket chat API | 8080 |
| `test-mono-chat-ui` | Thymeleaf chat UI served by Spring Boot | 8081 |

## Prerequisites

- JDK 21
- Gradle 8.x (or use the included Gradle Wrapper)

## Build

```bash
./gradlew build
```

## Run

Start the API module:

```bash
./gradlew :test-mono-chat-api:bootRun
```

Start the UI module (in a separate terminal):

```bash
./gradlew :test-mono-chat-ui:bootRun
```

Then open your browser at [http://localhost:8081](http://localhost:8081).

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/chat/messages?limit=50` | Retrieve recent chat messages |
| `POST` | `/api/chat/messages` | Send a new message |

### POST `/api/chat/messages` — Request body

```json
{
  "sender": "Alice",
  "content": "Hello, world!"
}
```

## WebSocket Usage

The API exposes a STOMP-over-SockJS endpoint at `ws://localhost:8080/ws`.

- **Subscribe** to `/topic/messages` to receive new messages in real time.
- **Send** a message by publishing to `/app/chat.send` with payload:

```json
{
  "sender": "Alice",
  "content": "Hello via WebSocket!"
}
```


