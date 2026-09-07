create database mpc;
-- 消息主表（单表继承策略）
CREATE TABLE messages (
    -- 基础字段
    id BIGINT PRIMARY KEY COMMENT '消息ID（雪花算法）',
    message_class VARCHAR(20) NOT NULL COMMENT '消息类型: PRIVATE, GROUP',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    message_type VARCHAR(20) NOT NULL COMMENT '消息类型: TEXT, IMAGE, FILE等',
    content TEXT COMMENT '消息内容',
    file_url VARCHAR(500) COMMENT '文件URL',
    sent_time DATETIME(3) NOT NULL COMMENT '发送时间（毫秒精度）',
    update_time DATETIME(3) NOT NULL COMMENT '更新时间',
    status VARCHAR(20) NOT NULL DEFAULT 'SENT' COMMENT '消息状态',
    version BIGINT DEFAULT 0 COMMENT '乐观锁版本号',
    
    -- 私聊特有字段（可为空）
    receiver_id BIGINT NULL COMMENT '接收者ID（私聊时使用）',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读（私聊）',
    read_time DATETIME(3) NULL COMMENT '阅读时间（私聊）',
    
    -- 群聊特有字段（可为空）
    group_id BIGINT NULL COMMENT '群组ID（群聊时使用）',
    is_at_all BOOLEAN DEFAULT FALSE COMMENT '是否@所有人',
    
    -- JSON字段（存储复杂数据）
    reactions JSON COMMENT '消息 reactions',
    mentioned_users JSON COMMENT '@的用户列表',
    friendshipsread_receipts JSON COMMENT '已读回执列表（群聊）',
    
    -- 索引
    INDEX idx_sender_id (sender_id),
    INDEX idx_sent_time (sent_time DESC),
    INDEX idx_status (status),
    
    -- 私聊专用索引
    INDEX idx_private_conversation (sender_id, receiver_id, sent_time) 
        WHERE message_class = 'PRIVATE',
    INDEX idx_receiver_unread (receiver_id, status, is_read) 
        WHERE message_class = 'PRIVATE',
    
    -- 群聊专用索引
    INDEX idx_group_messages (group_id, sent_time DESC) 
        WHERE message_class = 'GROUP',
    INDEX idx_group_status (group_id, status) 
        WHERE message_class = 'GROUP',
    
    -- 外键约束
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE RESTRICT,
    
    -- 数据完整性约束
    CONSTRAINT chk_target CHECK (
        (message_class = 'PRIVATE' AND receiver_id IS NOT NULL AND group_id IS NULL) OR
        (message_class = 'GROUP' AND group_id IS NOT NULL AND receiver_id IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='聊天消息表';