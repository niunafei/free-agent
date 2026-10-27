package com.webmodel.agent.application;

import com.webmodel.agent.common.BusinessException;
import com.webmodel.agent.common.ErrorCode;
import com.webmodel.agent.persistence.User;
import com.webmodel.agent.persistence.UserRepository;
import com.webmodel.agent.security.JwtUtil;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> register(String username, String password, String displayName) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(hash(password));
        u.setDisplayName(displayName != null ? displayName : username);
        u.setCreatedAt(LocalDateTime.now());
        userRepository.save(u);
        String token = jwtUtil.generate(u.getId(), u.getUsername());
        return Map.of("token", token, "userId", u.getId(), "username", u.getUsername(), "displayName", u.getDisplayName());
    }

    public Map<String, Object> login(String username, String password) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在"));
        if (!u.getPasswordHash().equals(hash(password))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "密码错误");
        }
        String token = jwtUtil.generate(u.getId(), u.getUsername());
        return Map.of("token", token, "userId", u.getId(), "username", u.getUsername(), "displayName", u.getDisplayName());
    }

    public Map<String, Object> me(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return Map.of("userId", u.getId(), "username", u.getUsername(), "displayName", u.getDisplayName(),
                "email", u.getEmail() != null ? u.getEmail() : "", "phone", u.getPhone() != null ? u.getPhone() : "");
    }

    private String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}