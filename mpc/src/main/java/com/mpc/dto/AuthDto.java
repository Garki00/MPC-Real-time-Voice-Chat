package com.mpc.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

public class AuthDto {

    @Data
    public static class RegisterRequest {
        @NotBlank
        @Size(min = 3, max = 50)
        private String username;

        @NotBlank
        @Size(min = 6, max = 100)
        private String password;

        @NotBlank
        @Email
        private String email;
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;

        @NotBlank
        private String password;
    }

    @Data
    public static class LoginResponse {
        private String token;
        private Long userId;
        private String username;
        private String avatar;

        public LoginResponse(String token, Long userId, String username, String avatar) {
            this.token = token;
            this.userId = userId;
            this.username = username;
            this.avatar = avatar;
        }
    }
}
