package com.example.campusactivity.service;

import com.example.campusactivity.dto.RegisterForm;
import com.example.campusactivity.entity.User;
import com.example.campusactivity.exception.BusinessException;
import com.example.campusactivity.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterForm form) {
        String username = form.getUsername().trim();
        String name = form.getName().trim();
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User(
                username,
                passwordEncoder.encode(form.getPassword()),
                name,
                form.getRole()
        );
        return userRepository.save(user);
    }

    public User authenticate(String username, String rawPassword) {
        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        return user;
    }

    public User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }
}
