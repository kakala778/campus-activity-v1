package com.example.campusactivity.service;

import com.example.campusactivity.dto.RegisterForm;
import com.example.campusactivity.entity.User;
import com.example.campusactivity.entity.UserRole;
import com.example.campusactivity.exception.BusinessException;
import com.example.campusactivity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void registerTrimsFieldsAndStoresBcryptHash() {
        RegisterForm form = registerForm("  student01  ", "plain-password", "  测试学生  ", UserRole.STUDENT);
        when(userRepository.existsByUsername("student01")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.register(form);

        assertThat(saved.getUsername()).isEqualTo("student01");
        assertThat(saved.getName()).isEqualTo("测试学生");
        assertThat(saved.getRole()).isEqualTo(UserRole.STUDENT);
        assertThat(saved.getPasswordHash()).isNotEqualTo("plain-password");
        assertThat(passwordEncoder.matches("plain-password", saved.getPasswordHash())).isTrue();
        verify(userRepository).existsByUsername("student01");
    }

    @Test
    void registerRejectsDuplicateUsername() {
        RegisterForm form = registerForm("existing", "password", "学生", UserRole.STUDENT);
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(form))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名已存在");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void authenticateReturnsUserForMatchingPassword() {
        User user = new User("teacher01", passwordEncoder.encode("correct-password"), "测试教师", UserRole.TEACHER);
        when(userRepository.findByUsername("teacher01")).thenReturn(Optional.of(user));

        User authenticated = userService.authenticate("teacher01", "correct-password");

        assertThat(authenticated).isSameAs(user);
    }

    @Test
    void authenticateRejectsWrongPassword() {
        User user = new User("student01", passwordEncoder.encode("correct-password"), "测试学生", UserRole.STUDENT);
        when(userRepository.findByUsername("student01")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.authenticate("student01", "wrong-password"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    void getRequiredUserRejectsMissingUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getRequiredUser(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在");
    }

    private RegisterForm registerForm(String username, String password, String name, UserRole role) {
        RegisterForm form = new RegisterForm();
        form.setUsername(username);
        form.setPassword(password);
        form.setName(name);
        form.setRole(role);
        return form;
    }
}
