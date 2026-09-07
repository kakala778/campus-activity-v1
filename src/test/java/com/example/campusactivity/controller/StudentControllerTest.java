package com.example.campusactivity.controller;

import com.example.campusactivity.dto.RegistrationStatusView;
import com.example.campusactivity.entity.Activity;
import com.example.campusactivity.entity.ActivityStatus;
import com.example.campusactivity.entity.User;
import com.example.campusactivity.entity.UserRole;
import com.example.campusactivity.exception.BusinessException;
import com.example.campusactivity.service.ActivityService;
import com.example.campusactivity.service.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    private static final Long ACTIVITY_ID = 100L;

    @Mock
    private ActivityService activityService;

    @Mock
    private RegistrationService registrationService;

    private StudentController controller;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        controller = new StudentController(activityService, registrationService);
        session = new MockHttpSession();
        session.setAttribute("LOGIN_USER_ID", 30L);
    }

    @Test
    void listShowsOnlyPublishedServiceResults() {
        Activity published = activity(ActivityStatus.PUBLISHED);
        when(activityService.listPublishedActivities()).thenReturn(List.of(published));
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.list(model);

        assertThat(view).isEqualTo("student/activities");
        assertThat(model.getAttribute("activities")).isEqualTo(List.of(published));
    }

    @Test
    void publishedDetailIsDisplayed() {
        Activity published = activity(ActivityStatus.PUBLISHED);
        when(activityService.getPublishedActivity(ACTIVITY_ID)).thenReturn(published);
        RegistrationStatusView registrationStatus = new RegistrationStatusView(2L, false, false, false);
        when(registrationService.getStatus(30L, published)).thenReturn(registrationStatus);
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.detail(ACTIVITY_ID, session, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("student/activity-detail");
        assertThat(model.getAttribute("activity")).isSameAs(published);
        assertThat(model.getAttribute("registrationStatus")).isSameAs(registrationStatus);
    }

    @Test
    void unavailableDetailRedirectsWithoutExposingActivity() {
        when(activityService.getPublishedActivity(ACTIVITY_ID))
                .thenThrow(new BusinessException("活动不存在或未发布"));
        ConcurrentModel model = new ConcurrentModel();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.detail(ACTIVITY_ID, session, model, redirect);

        assertThat(view).isEqualTo("redirect:/student/activities");
        assertThat(model.containsAttribute("activity")).isFalse();
        assertThat(redirect.getFlashAttributes().get("error")).isEqualTo("活动不存在或未发布");
    }

    @Test
    void successfulRegistrationRedirectsToDetailWithSuccessMessage() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.register(ACTIVITY_ID, session, redirect);

        verify(registrationService).register(30L, ACTIVITY_ID);
        assertThat(view).isEqualTo("redirect:/student/activities/" + ACTIVITY_ID);
        assertThat(redirect.getFlashAttributes().get("success")).isEqualTo("报名成功");
    }

    @Test
    void rejectedRegistrationRedirectsToDetailWithBusinessReason() {
        doThrow(new BusinessException("活动报名人数已满"))
                .when(registrationService).register(30L, ACTIVITY_ID);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.register(ACTIVITY_ID, session, redirect);

        assertThat(view).isEqualTo("redirect:/student/activities/" + ACTIVITY_ID);
        assertThat(redirect.getFlashAttributes().get("error")).isEqualTo("活动报名人数已满");
    }

    private Activity activity(ActivityStatus status) {
        User teacher = new User("teacher", "hash", "测试教师", UserRole.TEACHER);
        ReflectionTestUtils.setField(teacher, "id", 10L);
        Activity activity = new Activity(
                "测试活动", "活动说明", "教学楼 A101",
                LocalDateTime.of(2026, 10, 1, 12, 0),
                LocalDateTime.of(2026, 10, 2, 9, 0),
                LocalDateTime.of(2026, 10, 2, 11, 0),
                30, teacher
        );
        activity.setStatus(status);
        ReflectionTestUtils.setField(activity, "id", ACTIVITY_ID);
        return activity;
    }
}
