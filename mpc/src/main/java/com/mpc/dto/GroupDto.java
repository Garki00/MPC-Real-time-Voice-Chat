package com.mpc.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

public class GroupDto {

    @Data
    public static class CreateRequest {
        @NotBlank
        @Size(max = 100)
        private String name;
        private String avatar;
    }

    @Data
    public static class UpdateRequest {
        @Size(max = 100)
        private String name;
        private String announcement;
        private String avatar;
    }

    @Data
    public static class GroupInfo {
        private Long id;
        private String name;
        private Long ownerId;
        private String avatar;
        private String announcement;
        private int memberCount;
        private String createdAt;
        private String ownerName;
    }

    @Data
    public static class AdminRequest {
        @NotNull
        private Long userId;
    }

    @Data
    public static class KickRequest {
        @NotNull
        private Long userId;
    }

    @Data
    public static class MemberInfo {
        private Long userId;
        private String username;
        private String avatar;
        private String role;
        private String status;
    }

    @Data
    public static class JoinRequestInfo {
        private Long id;
        private Long groupId;
        private Long userId;
        private String username;
        private String avatar;
        private String createdAt;
    }
}
