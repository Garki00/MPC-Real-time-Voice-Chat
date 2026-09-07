package com.mpc.dto;

import com.mpc.model.Message.MessageType;
import lombok.Data;
import jakarta.validation.constraints.*;

public class ChatDto {

    @Data
    public static class PrivateMessage {
        @NotNull
        private Long receiverId;

        @NotBlank
        private String content;

        private MessageType type = MessageType.TEXT;
    }

    @Data
    public static class GroupMessage {
        @NotNull
        private Long groupId;

        @NotBlank
        private String content;

        private MessageType type = MessageType.TEXT;
    }

    @Data
    public static class MessagePayload {
        private Long id;
        private Long senderId;
        private String senderName;
        private String senderAvatar;
        private Long receiverId;
        private Long groupId;
        private String content;
        private MessageType type;
        private String createdAt;
    }
}
