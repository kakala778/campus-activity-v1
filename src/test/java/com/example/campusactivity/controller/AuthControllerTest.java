package com.example.campusactivity.controller;

import com.example.campusactivity.dto.LoginForm;
import com.example.campusactivity.dto.RegisterForm;
import com.example.campusactivity.entity.User;
import com.example.campusactivity.entity.UserRole;
import com.example.campusactivity.exception.BusinessException;
import com.example.campusactivity.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(userService);
    }

    @Test
    void studentLoginStoresSessionAndRedirectsToStudentHome() {
        User student = user(10L, "student01", "测试学生", UserRole.STUDENT);
        when(userService.authenticate("student01", "password")).thenReturn(student);
        LoginForm form = loginForm("student01", "password");
        MockHttpServletRequest request = new MockHttpServletRequest();

        String view = controller.login(form, bindingResult(form), request, new ConcurrentModel());

        assertThat(view).isEqualTo("redirect:/student/activities");
        assertThat(request.getSession().getAttribute("LOGIN_USER_ID")).isEqualTo(10L);
        assertThat(request.getSession().getAttribute("LOGIN_USER_NAME")).isEqualTo("测试学生");
        assertThat(request.getSession().getAttribute("LOGIN_USER_ROLE")).isEqualTo(UserRole.STUDENT);
    }

    @Test
    void teacherLoginRedirectsToTeacherHome() {
        User teacher = user(20L, "teacher01", "测试教师", UserRole.TEACHER);
        when(userService.authenticate("teacher01", "password")).thenReturn(teacher);
        LoginForm form = loginForm("teacher01", "password");

        String view = controller.login(form, bindingResult(form), new MockHttpServletRequest(), new ConcurrentModel());

        assertThat(view).isEqualTo("redirect:/teacher/activities");
    }

    @Test
    void invalidLoginReturnsPageWithGenericError() {
        LoginForm form = loginForm("student01", "wrong");
        when(userService.authenticate("student01", "wrong"))
                .thenThrow(new BusinessException("用户名或密码错误"));
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.login(form, bindingResult(form), new MockHttpServletRequest(), model);

        assertThat(view).isEqualTo("auth/login");
        assertThat(model.getAttribute("error")).isEqualTo("用户名或密码错误");
    }

    @Test
    void successfulRegistrationRedirectsToLoginWithMessage() {
        RegisterForm form = registerForm("student01", UserRole.STUDENT);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.register(form, bindingResult(form), new ConcurrentModel(), redirect);

        assertThat(view).isEqualTo("redirect:/login");
        assertThat(redirect.getFlashAttributes().get("success")).isEqualTo("注册成功，请登录");
    }

    @Test
    void duplicateRegistrationReturnsPageWithError() {
        RegisterForm form = registerForm("student01", UserRole.STUDENT);
        doThrow(new BusinessException("用户名已存在")).when(userService).register(form);
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.register(form, bindingResult(form), model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("auth/register");
        assertThat(model.getAttribute("error")).isEqualTo("用户名已存在");
    }

    @Test
    void logoutInvalidatesExistingSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();
        session.setAttribute("LOGIN_USER_ID", 1L);

        String view = controller.logout(request);

        assertThat(view).isEqualTo("redirect:/login");
        assertThat(session.isInvalid()).isTrue();
    }

    private User user(Long id, String username, String name, UserRole role) {
        User user = new User(username, "hash", name, role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private LoginForm loginForm(String username, String password) {
        LoginForm form = new LoginForm();
        form.setUsername(username);
        form.setPassword(password);
        return form;
    }

    private RegisterForm registerForm(String username, UserRole role) {
        RegisterForm form = new RegisterForm();
        form.setUsername(username);
        form.setPassword("password");
        form.setName("测试用户");
        form.setRole(role);
        return form;
    }

    private <T> BeanPropertyBindingResult bindingResult(T form) {
        return new BeanPropertyBindingResult(form, "form");
    }
}
