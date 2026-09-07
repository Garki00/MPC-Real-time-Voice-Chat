package com.mpc.service;

import com.mpc.dto.UserDto;
import com.mpc.model.User;
import com.mpc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OnlineStatusService onlineStatusService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserDto.UserInfo getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return toInfo(user);
    }

    @Transactional
    public UserDto.UserInfo updateProfile(Long userId, UserDto.UpdateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (req.getUsername() != null && !req.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(req.getUsername())) {
                throw new IllegalArgumentException("用户名已存在");
            }
            user.setUsername(req.getUsername());
        }
        if (req.getEmail() != null && !req.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(req.getEmail())) {
                throw new IllegalArgumentException("邮箱已被注册");
            }
            user.setEmail(req.getEmail());
        }
        if (req.getAvatar() != null) {
            user.setAvatar(req.getAvatar());
        }
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        return toInfo(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserDto.UserInfo toInfo(User user) {
        boolean online = onlineStatusService.isOnline(user.getId());
        User.UserStatus status = online ? User.UserStatus.ONLINE : User.UserStatus.OFFLINE;
        return new UserDto.UserInfo(user.getId(), user.getUsername(), user.getEmail(), user.getAvatar(), status);
    }
}
