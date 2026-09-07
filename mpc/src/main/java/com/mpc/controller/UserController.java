package com.mpc.controller;

import com.mpc.dto.UserDto;
import com.mpc.model.User;
import com.mpc.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto.UserInfo> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getUser(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto.UserInfo> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto.UserInfo> update(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UserDto.UpdateRequest req) {
        return ResponseEntity.ok(userService.updateProfile(user.getId(), req));
    }
}
