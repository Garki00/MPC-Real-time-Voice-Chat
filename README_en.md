# MPC - Real-time Voice Chat Platform

A full-stack web application for real-time voice communication with text messaging, friend system, and group management. Built with Spring Boot 3.2 backend and designed for WebRTC-based voice channels.

[![Java](https://img.shields.io/badge/Java-17-red.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## Features

- **User Authentication** - JWT-based secure login/registration with Spring Security
- **Friend Management** - Send/accept friend requests, manage friend lists
- **Group Chat** - Create groups, manage members, role-based permissions (owner/admin/member)
- **Private Messaging** - Real-time text chat between friends via STOMP
- **Voice Channels** - WebRTC-powered voice rooms within groups (up to 10 participants)
- **Real-time Notifications** - Friend requests, group invites, announcements
- **Online Status** - Redis-backed presence tracking
- **File Upload** - Avatar management with multipart support

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.2.5
- **Security**: Spring Security + JWT (JJWT 0.12.5)
- **Database**: MySQL 8.0 (JPA/Hibernate)
- **Cache**: Redis (Lettuce client)
- **WebSocket**: STOMP over SockJS + Native WebSocket for signaling
- **Build**: Maven

### Real-time Communication
- **Text Chat**: STOMP messaging protocol
- **Voice**: WebRTC peer-to-peer audio streams
- **Signaling**: Custom WebSocket handler at `/ws/signal`

### Redis Usage
- **Online Status Tracking**: Stores user online status with key format `user:online:{userId}`, value as timestamp
- **Voice Channel State**: Tracks which voice channel a user is currently in, key format `user:voice:{userId}`
- **Session Management**: Stores JWT token blacklist for logout functionality
- **Caching Layer**: Caches hot data to reduce database query load

## Project Structure

```
mpc/
├── src/main/java/com/mpc/
│   ├── config/              # Security, WebSocket, Redis, CORS, global exception handling
│   ├── controller/          # REST endpoints and STOMP message mappings
│   ├── dto/                 # Request/response data transfer objects
│   ├── model/               # JPA entities (User, Group, VoiceChannel, Message, etc.)
│   ├── repository/          # Spring Data JPA repositories
│   ├── security/            # JWT authentication filter
│   ├── service/             # Business logic layer
│   ├── util/                # JWT utility class
│   └── websocket/           # WebRTC signaling handler
├── src/main/resources/
│   ├── application.yml      # Configuration file
│   └── schema.sql           # Database schema (optional, JPA auto-creates tables)
└── uploads/avatars/         # User avatar storage
```

## Getting Started

### Option 1: Using Docker (Recommended)

#### Prerequisites

- Docker 20.10+
- Docker Compose 2.0+

#### Start All Services

1. Clone the project and navigate to directory:
```bash
git clone <repository-url>
cd voice
```

2. Start with Docker Compose:
```bash
docker-compose up -d
```

This will automatically start the following services:
- **MySQL** - Running on `localhost:3306`
- **Redis** - Running on `localhost:6379`
- **Backend Service** - Running on `localhost:8080`
- **Frontend Service** - Running on `localhost:80`

3. Access the application:
Open your browser and visit `http://localhost`

#### File Storage

Uploaded files (such as user avatars) are stored in the `./data/uploads` directory on the host machine, mapped to `/app/uploads` in the backend container. Files persist on the host even if containers are restarted or removed.

#### Stop Services

```bash
docker-compose down
```

To also remove data volumes:
```bash
docker-compose down -v
```

### Option 2: Local Development Environment

#### Prerequisites

- Java 17+
- MySQL 8.0
- Redis 6.0+
- Maven 3.6+
- Node.js 16+

#### Database Setup

1. Create database:
```sql
CREATE DATABASE mpc DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. (Optional) Run `src/main/resources/schema.sql` to create tables manually, or let JPA handle it via `ddl-auto: update`

3. Update `mpc/src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/mpc
    username: your_username
    password: your_password
  data:
    redis:
      host: 127.0.0.1
      port: 6379
```

#### Run Backend

```bash
cd mpc
mvn spring-boot:run
```

Backend server runs at `http://localhost:8080`

#### Run Frontend

```bash
cd mpc-frontend
npm install
npm run dev
```

Frontend server runs at `http://localhost:5173`

## API Documentation

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Register new user | No |
| POST | `/api/auth/login` | Login and receive JWT | No |
| POST | `/api/auth/logout` | Logout (clear Redis session) | Yes |

### User Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/me` | Get current user profile |
| PUT | `/api/users/me` | Update profile (username, email) |
| GET | `/api/users/{id}` | Get user info by ID |
| POST | `/api/upload/avatar` | Upload avatar image (multipart/form-data) |

### Friends

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/friends` | List all accepted friends |
| GET | `/api/friends/requests` | List pending friend requests |
| POST | `/api/friends/request/{targetId}` | Send friend request |
| POST | `/api/friends/request/{requesterId}/handle?action=accept` | Accept/reject request |
| DELETE | `/api/friends/{friendId}` | Remove friend |

### Groups

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/groups/mine` | List user's groups |
| POST | `/api/groups` | Create new group |
| DELETE | `/api/groups/{groupId}` | Dissolve group (owner only) |
| PUT | `/api/groups/{groupId}` | Update group name/announcement |
| GET | `/api/groups/{groupId}/members` | List group members |
| POST | `/api/groups/{groupId}/join` | Request to join group |
| POST | `/api/groups/{groupId}/leave` | Leave group |
| GET | `/api/groups/{groupId}/requests` | List join requests (admin only) |
| POST | `/api/groups/requests/{requestId}/handle?action=approve` | Approve/reject join request |
| POST | `/api/groups/{groupId}/kick` | Kick member (admin only) |
| POST | `/api/groups/{groupId}/admin?userId=X&grant=true` | Set/revoke admin role (owner only) |

### Voice Channels

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/groups/{groupId}/channels` | List voice channels in group |
| POST | `/api/groups/{groupId}/channels` | Create voice channel |
| PUT | `/api/channels/{channelId}` | Update channel name/capacity |
| DELETE | `/api/channels/{channelId}` | Delete channel (admin only) |

### Chat History

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/chat/private/{friendId}` | Fetch private message history |
| GET | `/api/chat/group/{groupId}` | Fetch group message history |

## WebSocket Endpoints

### STOMP Messaging (`/ws/chat`)

**Client sends to:**
- `/app/chat.private` - Send private message
- `/app/chat.group` - Send group message
- `/app/voice.join` - Join voice channel
- `/app/voice.leave` - Leave voice channel

**Client subscribes to:**
- `/user/queue/messages` - Receive private messages
- `/topic/group/{groupId}` - Receive group messages
- `/topic/voice/{groupId}` - Voice channel state updates
- `/user/queue/notifications` - System notifications (friend requests, kicks, announcements, etc.)

### WebRTC Signaling (`/ws/signal`)

Raw WebSocket for exchanging SDP offers/answers and ICE candidates.

**Message format:**
```json
{
  "type": "offer|answer|ice",
  "from": "userId",
  "to": "targetUserId",
  "channelId": "voiceChannelId",
  "payload": { /* SDP or ICE candidate */ }
}
```

## Data Models

### Core Entities

- **User** - Authentication, profile, online status
- **Friendship** - Bidirectional friend relationships with PENDING/ACCEPTED/REJECTED status
- **Group** - Chat groups with owner and announcement
- **GroupMember** - Many-to-many with role (OWNER/ADMIN/MEMBER)
- **GroupJoinRequest** - Approval workflow for joining groups
- **VoiceChannel** - Voice rooms within groups (max 10 participants)
- **Message** - Text messages for private or group chat

## Configuration

### JWT Settings

Update in `application.yml`:
```yaml
mpc:
  jwt:
    secret: your-secret-key-minimum-32-bytes
    expiration: 86400000  # 24 hours in milliseconds
```

### File Upload

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

mpc:
  upload:
    avatar-dir: uploads/avatars/
```

## Development Notes

- **Lombok**: IDE plugin required for annotation processing
- **WebRTC**: Currently configured for localhost testing. For production, configure TURN/STUN servers (e.g., Coturn)
- **Voice Streams**: P2P audio only, no server recording/storage
- **User Limits**: Max 10 participants per voice channel (enforced by backend)
- **Concurrency**: Users can only join one voice channel at a time

## Security

- Passwords hashed with BCrypt
- JWT tokens validated on every request via `JwtAuthFilter`
- WebSocket connections authenticated via interceptor
- CORS configured for local development (adjust for production)

## Frontend Integration

This backend is designed to work with a Vue 3 + Vite frontend. Key integration points:

1. Store JWT from `/api/auth/login` in localStorage
2. Include `Authorization: Bearer {token}` header in HTTP requests
3. Connect STOMP client to `ws://localhost:8080/ws/chat` with JWT in headers
4. Connect WebRTC signaling to `ws://localhost:8080/ws/signal?token={jwt}`
5. Handle notifications via `/user/queue/notifications` subscription

## License

MIT

## Contributing

Contributions welcome! Please open an issue or submit a pull request.

---

**Built with ❤️ using Spring Boot and WebRTC**
