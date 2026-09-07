package com.mpc.service;

import com.mpc.dto.AuthDto;
import com.mpc.model.User;
import com.mpc.repository.UserRepository;
import com.mpc.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OnlineStatusService onlineStatusService;

    @Transactional
    public void register(AuthDto.RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("邮箱已被注册");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        userRepository.save(user);
    }

    @Transactional
    public AuthDto.LoginResponse login(AuthDto.LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        user.setStatus(User.UserStatus.ONLINE);
        userRepository.save(user);
        onlineStatusService.setOnline(user.getId());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new AuthDto.LoginResponse(token, user.getId(), user.getUsername(), user.getAvatar());
    }

    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(User.UserStatus.OFFLINE);
            userRepository.save(user);
        });
        onlineStatusService.setOffline(userId);
    }
}
