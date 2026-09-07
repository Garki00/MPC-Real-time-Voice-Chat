package com.mpc.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

public class VoiceChannelDto {

    @Data
    public static class CreateRequest {
        @NotBlank
        @Size(max = 100)
        private String name;

        @Min(1) @Max(10)
        private Integer maxCapacity = 10;
    }

    @Data
    public static class UpdateRequest {
        @Size(max = 100)
        private String name;

        @Min(1) @Max(10)
        private Integer maxCapacity;
    }

    @Data
    public static class ParticipantInfo {
        private Long userId;
        private String username;
        private String avatar;
    }

    @Data
    public static class ChannelInfo {
        private Long id;
        private Long groupId;
        private String name;
        private int maxCapacity;
        private int currentCount;
        private java.util.List<ParticipantInfo> participants;
    }
}
