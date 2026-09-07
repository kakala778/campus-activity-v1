package com.example.campusactivity.config;

import com.example.campusactivity.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthInterceptorTest {

    private final AuthInterceptor interceptor = new AuthInterceptor();

    @Test
    void anonymousBusinessRequestRedirectsToLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/student/activities");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/login");
    }

    @Test
    void studentCannotAccessTeacherPath() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("/teacher/activities", UserRole.STUDENT);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void teacherCannotAccessStudentPath() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("/student/activities", UserRole.TEACHER);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void studentCanAccessStudentPath() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("/student/activities", UserRole.STUDENT);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    @Test
    void teacherCanAccessTeacherPath() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("/teacher/activities", UserRole.TEACHER);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    private MockHttpServletRequest authenticatedRequest(String path, UserRole role) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.getSession().setAttribute("LOGIN_USER_ID", 1L);
        request.getSession().setAttribute("LOGIN_USER_ROLE", role);
        return request;
    }
}
