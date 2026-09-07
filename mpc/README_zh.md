# MPC - 实时语音聊天平台

一个全栈 Web 应用，支持实时语音通话、文字消息、好友系统与群组管理。后端基于 Spring Boot 3.2 构建，面向基于 WebRTC 的语音频道设计。

[![Java](https://img.shields.io/badge/Java-17-red.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## 功能特性

- **用户认证** - 基于 JWT 的安全登录/注册，集成 Spring Security
- **好友管理** - 发送/接受好友请求，管理好友列表
- **群组聊天** - 创建群组、管理成员、基于角色的权限（群主/管理员/成员）
- **私信聊天** - 通过 STOMP 实现好友间的实时文字聊天
- **语音频道** - 基于 WebRTC 的群组内语音房间（最多 10 人同时在线）
- **实时通知** - 好友请求、群组邀请、公告等
- **在线状态** - 基于 Redis 的在线状态追踪
- **文件上传** - 支持 multipart 的头像管理

## 技术栈

### 后端
- **框架**: Spring Boot 3.2.5
- **安全**: Spring Security + JWT（JJWT 0.12.5）
- **数据库**: MySQL 8.0（JPA/Hibernate）
- **缓存**: Redis（Lettuce 客户端）
- **WebSocket**: 基于 SockJS 的 STOMP 协议 + 原生 WebSocket 信令
- **构建工具**: Maven

### 实时通信
- **文字聊天**: STOMP 消息协议
- **语音**: WebRTC 点对点音频流
- **信令**: 位于 `/ws/signal` 的自定义 WebSocket 处理器

### Redis 用途
- **在线状态追踪**: 存储用户在线状态，键格式 `user:online:{userId}`，值为时间戳
- **语音频道状态**: 追踪用户当前所在的语音频道，键格式 `user:voice:{userId}`
- **会话管理**: 存储 JWT 令牌的黑名单（用于登出功能）
- **缓存层**: 缓存热点数据以减少数据库查询压力

## 项目结构

```
mpc/
├── src/main/java/com/mpc/
│   ├── config/              # 安全、WebSocket、Redis、CORS、全局异常处理
│   ├── controller/          # REST 接口与 STOMP 消息映射
│   ├── dto/                 # 请求/响应数据传输对象
│   ├── model/               # JPA 实体（User、Group、VoiceChannel、Message 等）
│   ├── repository/          # Spring Data JPA 数据仓库
│   ├── security/            # JWT 认证过滤器
│   ├── service/             # 业务逻辑层
│   ├── util/                # JWT 工具类
│   └── websocket/           # WebRTC 信令处理器
├── src/main/resources/
│   ├── application.yml      # 配置文件
│   └── schema.sql           # 数据库表结构（可选，JPA 可自动建表）
└── uploads/avatars/         # 用户头像存储目录
```

## 快速开始

### 环境要求

- Java 17+
- MySQL 8.0
- Redis 6.0+
- Maven 3.6+

### 数据库配置

1. 创建数据库：
```sql
CREATE DATABASE mpc DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. （可选）手动执行 `src/main/resources/schema.sql` 创建数据表，或通过 `ddl-auto: update` 交由 JPA 自动建表

3. 修改 `src/main/resources/application.yml`：
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

### 运行应用

```bash
cd mpc
mvn spring-boot:run
```

服务运行于 `http://localhost:8080`

## API 文档

### 认证接口

| 方法 | 接口 | 说明 | 是否需要认证 |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | 注册新用户 | 否 |
| POST | `/api/auth/login` | 登录并获取 JWT | 否 |
| POST | `/api/auth/logout` | 退出登录（清除 Redis 会话） | 是 |

### 用户管理

| 方法 | 接口 | 说明 |
|--------|----------|-------------|
| GET | `/api/users/me` | 获取当前用户信息 |
| PUT | `/api/users/me` | 更新个人资料（用户名、邮箱） |
| GET | `/api/users/{id}` | 按 ID 获取用户信息 |
| POST | `/api/upload/avatar` | 上传头像图片（multipart/form-data） |

### 好友管理

| 方法 | 接口 | 说明 |
|--------|----------|-------------|
| GET | `/api/friends` | 获取所有已添加的好友列表 |
| GET | `/api/friends/requests` | 获取待处理的好友请求列表 |
| POST | `/api/friends/request/{targetId}` | 发送好友请求 |
| POST | `/api/friends/request/{requesterId}/handle?action=accept` | 接受/拒绝好友请求 |
| DELETE | `/api/friends/{friendId}` | 删除好友 |

### 群组管理

| 方法 | 接口 | 说明 |
|--------|----------|-------------|
| GET | `/api/groups/mine` | 获取用户加入的群组列表 |
| POST | `/api/groups` | 创建新群组 |
| DELETE | `/api/groups/{groupId}` | 解散群组（仅群主） |
| PUT | `/api/groups/{groupId}` | 更新群名称/公告 |
| GET | `/api/groups/{groupId}/members` | 获取群成员列表 |
| POST | `/api/groups/{groupId}/join` | 申请加入群组 |
| POST | `/api/groups/{groupId}/leave` | 退出群组 |
| GET | `/api/groups/{groupId}/requests` | 获取入群申请列表（仅管理员） |
| POST | `/api/groups/requests/{requestId}/handle?action=approve` | 通过/拒绝入群申请 |
| POST | `/api/groups/{groupId}/kick` | 踢出成员（仅管理员） |
| POST | `/api/groups/{groupId}/admin?userId=X&grant=true` | 设置/取消管理员角色（仅群主） |

### 语音频道

| 方法 | 接口 | 说明 |
|--------|----------|-------------|
| GET | `/api/groups/{groupId}/channels` | 获取群内语音频道列表 |
| POST | `/api/groups/{groupId}/channels` | 创建语音频道 |
| PUT | `/api/channels/{channelId}` | 更新频道名称/容量 |
| DELETE | `/api/channels/{channelId}` | 删除频道（仅管理员） |

### 聊天记录

| 方法 | 接口 | 说明 |
|--------|----------|-------------|
| GET | `/api/chat/private/{friendId}` | 获取私聊消息历史 |
| GET | `/api/chat/group/{groupId}` | 获取群聊消息历史 |

## WebSocket 接口

### STOMP 消息（`/ws/chat`）

**客户端发送至：**
- `/app/chat.private` - 发送私聊消息
- `/app/chat.group` - 发送群聊消息
- `/app/voice.join` - 加入语音频道
- `/app/voice.leave` - 离开语音频道

**客户端订阅：**
- `/user/queue/messages` - 接收私聊消息
- `/topic/group/{groupId}` - 接收群聊消息
- `/topic/voice/{groupId}` - 语音频道状态更新
- `/user/queue/notifications` - 系统通知（好友请求、被踢出、公告等）

### WebRTC 信令（`/ws/signal`）

用于交换 SDP offer/answer 和 ICE candidate 的原生 WebSocket。

**消息格式：**
```json
{
  "type": "offer|answer|ice",
  "from": "userId",
  "to": "targetUserId",
  "channelId": "voiceChannelId",
  "payload": { /* SDP 或 ICE candidate */ }
}
```

## 数据模型

### 核心实体

- **User（用户）** - 认证信息、个人资料、在线状态
- **Friendship（好友关系）** - 双向好友关系，状态包括 PENDING/ACCEPTED/REJECTED（待处理/已接受/已拒绝）
- **Group（群组）** - 聊天群组，包含群主与公告
- **GroupMember（群成员）** - 多对多关系，角色包括 OWNER/ADMIN/MEMBER（群主/管理员/成员）
- **GroupJoinRequest（入群申请）** - 入群审批流程
- **VoiceChannel（语音频道）** - 群内语音房间（最多 10 人同时在线）
- **Message（消息）** - 私聊或群聊的文字消息

## 配置说明

### JWT 设置

在 `application.yml` 中修改：
```yaml
mpc:
  jwt:
    secret: your-secret-key-minimum-32-bytes
    expiration: 86400000  # 24 小时（毫秒）
```

### 文件上传

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

## 开发注意事项

- **Lombok**: 需要 IDE 插件支持注解处理
- **WebRTC**: 当前配置仅用于 localhost 测试。生产环境需配置 TURN/STUN 服务器（如 Coturn）
- **语音流**: 仅支持 P2P 音频，服务器不进行录制或存储
- **人数限制**: 每个语音频道最多 10 人同时在线（由后端强制校验）
- **并发限制**: 用户同一时间只能加入一个语音频道

## 安全性

- 密码使用 BCrypt 加密存储
- 每个请求通过 `JwtAuthFilter` 校验 JWT 令牌
- WebSocket 连接通过拦截器进行认证
- 已为本地开发配置 CORS（生产环境需调整）

## 前端集成

该后端设计用于配合 Vue 3 + Vite 前端使用。关键集成点：

1. 将 `/api/auth/login` 返回的 JWT 存储到 localStorage
2. 在 HTTP 请求头中携带 `Authorization: Bearer {token}`
3. 连接 STOMP 客户端到 `ws://localhost:8080/ws/chat`，并在请求头中携带 JWT
4. 连接 WebRTC 信令到 `ws://localhost:8080/ws/signal?token={jwt}`
5. 通过订阅 `/user/queue/notifications` 处理通知

## 开源协议

MIT

## 参与贡献

欢迎贡献！请提交 issue 或 pull request。

---

**使用 Spring Boot 和 WebRTC 用心构建 ❤️**
