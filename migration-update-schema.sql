-- Migration Script: Add Message Read Status and Group Announcements
-- 迁移脚本：为现有数据库添加消息已读状态和群组通知功能
-- 执行日期：2026-09-07
-- 用途：在不影响现有数据的情况下升级数据库结构
-- 基于：mpc/src/main/resources/schema.sql

USE mpc;

-- ============================================
-- 1. 为 messages 表添加索引（优化查询性能）
-- ============================================
-- 添加发送者索引
ALTER TABLE messages
ADD INDEX idx_sender (sender_id);

-- 添加接收者索引（私聊查询优化）
ALTER TABLE messages
ADD INDEX idx_receiver (receiver_id, created_at DESC);

-- 添加群组索引（群聊查询优化）
ALTER TABLE messages
ADD INDEX idx_group (group_id, created_at DESC);

-- ============================================
-- 2. 创建群组通知表
-- ============================================
CREATE TABLE IF NOT EXISTS group_announcements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL COMMENT '群组ID',
    author_id BIGINT NOT NULL COMMENT '发布者ID',
    content VARCHAR(500) NOT NULL COMMENT '通知内容',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 索引
    INDEX idx_group_time (group_id, created_at DESC) COMMENT '按群组和时间查询通知',

    -- 外键约束
    FOREIGN KEY (group_id) REFERENCES `groups`(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='群组通知表';

-- ============================================
-- 3. 创建消息已读状态表（核心功能 - 跨设备同步）
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
-- 4. 为现有用户初始化已读状态（可选 - 避免旧消息显示为未读）
-- ============================================
-- 4.1 为每个用户的每个私聊会话创建初始已读状态记录
-- 策略：将最后已读消息ID设为该会话最新的消息ID（表示全部已读）
INSERT INTO message_read_status (user_id, conversation_type, conversation_id, last_read_message_id, last_read_at)
SELECT DISTINCT
    receiver_id AS user_id,
    'PRIVATE' AS conversation_type,
    sender_id AS conversation_id,
    (SELECT MAX(id) FROM messages m2
     WHERE (m2.sender_id = m1.sender_id AND m2.receiver_id = m1.receiver_id)
        OR (m2.sender_id = m1.receiver_id AND m2.receiver_id = m1.sender_id)
    ) AS last_read_message_id,
    NOW() AS last_read_at
FROM messages m1
WHERE m1.receiver_id IS NOT NULL
ON DUPLICATE KEY UPDATE last_read_message_id = VALUES(last_read_message_id);

-- 4.2 为每个用户的每个群组创建初始已读状态记录
INSERT INTO message_read_status (user_id, conversation_type, conversation_id, last_read_message_id, last_read_at)
SELECT DISTINCT
    gm.user_id,
    'GROUP' AS conversation_type,
    gm.group_id AS conversation_id,
    (SELECT MAX(id) FROM messages m WHERE m.group_id = gm.group_id) AS last_read_message_id,
    NOW() AS last_read_at
FROM group_members gm
WHERE EXISTS (SELECT 1 FROM messages m WHERE m.group_id = gm.group_id)
ON DUPLICATE KEY UPDATE last_read_message_id = VALUES(last_read_message_id);

-- ============================================
-- 5. 验证迁移结果
-- ============================================
SELECT
    'Migration completed successfully!' AS status,
    (SELECT COUNT(*) FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = 'mpc' AND TABLE_NAME = 'message_read_status') AS read_status_table_exists,
    (SELECT COUNT(*) FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = 'mpc' AND TABLE_NAME = 'group_announcements') AS announcements_table_exists,
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = 'mpc' AND TABLE_NAME = 'messages' AND INDEX_NAME = 'idx_sender') AS messages_idx_count,
    (SELECT COUNT(*) FROM message_read_status) AS read_status_records,
    (SELECT COUNT(*) FROM group_announcements) AS announcement_records;

-- ============================================
-- 使用说明
-- ============================================
-- 执行方式：
--   mysql -u root -p < migration-update-schema.sql
-- 或在 MySQL 客户端中：
--   source D:\AAAAGarki\Sonnet\voice\migration-update-schema.sql
--
-- 回滚方式（如需回滚）：
--   DROP TABLE IF EXISTS message_read_status;
--   DROP TABLE IF EXISTS group_announcements;
--   ALTER TABLE messages DROP INDEX IF EXISTS idx_sender;
--   ALTER TABLE messages DROP INDEX IF EXISTS idx_receiver;
--   ALTER TABLE messages DROP INDEX IF EXISTS idx_group;
