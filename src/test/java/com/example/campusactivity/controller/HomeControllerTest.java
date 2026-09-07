package com.example.campusactivity.controller;

import com.example.campusactivity.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;

class HomeControllerTest {

    private final HomeController controller = new HomeController();

    @Test
    void anonymousHomeRedirectsToLogin() {
        assertThat(controller.home(new MockHttpSession())).isEqualTo("redirect:/login");
    }

    @Test
    void studentHomeRedirectsToStudentActivities() {
        MockHttpSession session = session(UserRole.STUDENT);

        assertThat(controller.home(session)).isEqualTo("redirect:/student/activities");
    }

    @Test
    void teacherHomeRedirectsToTeacherActivities() {
        MockHttpSession session = session(UserRole.TEACHER);

        assertThat(controller.home(session)).isEqualTo("redirect:/teacher/activities");
    }

    private MockHttpSession session(UserRole role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("LOGIN_USER_ID", 1L);
        session.setAttribute("LOGIN_USER_ROLE", role);
        return session;
    }
}
