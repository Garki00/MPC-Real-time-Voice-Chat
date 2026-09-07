package com.mpc.dto;

import com.mpc.model.User.UserStatus;
import lombok.Data;
import jakarta.validation.constraints.*;

public class UserDto {

    @Data
    public static class UpdateRequest {
        @Size(max = 50)
        private String username;

        @Email
        private String email;

        private String avatar;

        @Size(min = 6, max = 100)
        private String password;
    }

    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String avatar;
        private UserStatus status;

        public UserInfo(Long id, String username, String email, String avatar, UserStatus status) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.avatar = avatar;
            this.status = status;
        }
    }
}
