-- MPC Database Schema v2
-- 新增功能：消息已读状态（跨设备同步）、群组通知
-- 更新日期：2026-09-07
-- 使用说明：全新部署时使用此文件替代 schema.sql

CREATE DATABASE IF NOT EXISTS mpc DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mpc;

-- ============================================
-- 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    avatar VARCHAR(255),
    status ENUM('ONLINE','OFFLINE') NOT NULL DEFAULT 'OFFLINE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 好友关系表
-- ============================================
CREATE TABLE IF NOT EXISTS friendships (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    status ENUM('PENDING','ACCEPTED','REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_pair (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 群组表
-- ============================================
CREATE TABLE IF NOT EXISTS `groups` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    owner_id BIGINT NOT NULL,
    avatar VARCHAR(255),
    announcement TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 群组成员表
-- ============================================
CREATE TABLE IF NOT EXISTS group_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role ENUM('OWNER','ADMIN','MEMBER') NOT NULL DEFAULT 'MEMBER',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_group_user (group_id, user_id),
    FOREIGN KEY (group_id) REFERENCES `groups`(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 群组加入申请表
-- ============================================
CREATE TABLE IF NOT EXISTS group_join_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_group_user_req (group_id, user_id),
    FOREIGN KEY (group_id) REFERENCES `groups`(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 语音频道表
-- ============================================
CREATE TABLE IF NOT EXISTS voice_channels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    max_capacity INT NOT NULL DEFAULT 10,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES `groups`(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 消息表（带性能优化索引）
-- ============================================
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT,
    group_id BIGINT,
    content TEXT NOT NULL,
    type ENUM('TEXT','IMAGE','FILE') NOT NULL DEFAULT 'TEXT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 外键约束
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (group_id) REFERENCES `groups`(id) ON DELETE CASCADE,

    -- 性能优化索引
    INDEX idx_sender (sender_id),
    INDEX idx_receiver (receiver_id, created_at DESC),
    INDEX idx_group (group_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 群组通知表（新增）
-- ============================================
CREATE TABLE IF NOT EXISTS group_announcements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL COMMENT '群组ID',
    author_id BIGINT NOT NULL COMMENT '发布者ID',
    content VARCHAR(500) NOT NULL COMMENT '通知内容',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 索引
    INDEX idx_group_time (group_id, created_at DESC),

    -- 外键约束
    FOREIGN KEY (group_id) REFERENCES `groups`(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='群组通知表';

-- ============================================
-- 消息已读状态表（新增 - 核心功能）
-- ============================================
CREATE TABLE IF NOT EXISTS message_read_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    conversation_type ENUM('PRIVATE', 'GROUP') NOT NULL COMMENT '会话类型：PRIVATE=私聊, GROUP=群聊',
    conversation_id BIGINT NOT NULL COMMENT '会话ID（私聊时为对方user_id，群聊时为group_id）',
    last_read_message_id BIGINT NULL COMMENT '最后已读的消息ID（NULL表示未读任何消息）',
    last_read_at TIMESTAMP NULL COMMENT '最后已读时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

    -- 唯一约束：每个用户对每个会话只有一条记录
    UNIQUE KEY uq_user_conversation (user_id, conversation_type, conversation_id),

    -- 索引优化
    INDEX idx_user_updated (user_id, updated_at DESC) COMMENT '查询用户所有会话的已读状态',
    INDEX idx_conversation (conversation_type, conversation_id) COMMENT '查询特定会话的已读情况',

    -- 外键约束
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (last_read_message_id) REFERENCES messages(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='消息已读状态表 - 支持跨设备同步未读消息计数';

-- ============================================
-- Schema 版本信息
-- ============================================
-- 版本：v2
-- 新增表：group_announcements, message_read_status
-- 新增索引：messages 表添加 idx_sender, idx_receiver, idx_group
-- 更新内容：支持跨设备消息已读状态同步
