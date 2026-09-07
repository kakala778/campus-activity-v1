package com.example.campusactivity.controller;

import com.example.campusactivity.dto.ActivityForm;
import com.example.campusactivity.entity.Activity;
import com.example.campusactivity.entity.ActivityStatus;
import com.example.campusactivity.entity.User;
import com.example.campusactivity.entity.UserRole;
import com.example.campusactivity.exception.BusinessException;
import com.example.campusactivity.service.ActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherControllerTest {

    private static final Long TEACHER_ID = 10L;
    private static final Long ACTIVITY_ID = 100L;

    @Mock
    private ActivityService activityService;

    private TeacherController controller;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        controller = new TeacherController(activityService);
        session = new MockHttpSession();
        session.setAttribute("LOGIN_USER_ID", TEACHER_ID);
    }

    @Test
    void listShowsCurrentTeachersActivities() {
        Activity draft = activity(ActivityStatus.DRAFT);
        when(activityService.listTeacherActivities(TEACHER_ID)).thenReturn(List.of(draft));
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.list(session, model);

        assertThat(view).isEqualTo("teacher/activities");
        assertThat(model.getAttribute("activities")).isEqualTo(List.of(draft));
    }

    @Test
    void createValidDraftRedirectsToDetail() {
        ActivityForm form = validForm();
        Activity draft = activity(ActivityStatus.DRAFT);
        when(activityService.createDraft(TEACHER_ID, form)).thenReturn(draft);

        String view = controller.create(
                form, bindingResult(form), session, new ConcurrentModel(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/teacher/activities/" + ACTIVITY_ID);
    }

    @Test
    void invalidCreateReturnsFormWithoutCallingService() {
        ActivityForm form = validForm();
        BeanPropertyBindingResult errors = bindingResult(form);
        errors.rejectValue("title", "NotBlank");

        String view = controller.create(
                form, errors, session, new ConcurrentModel(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("teacher/activity-form");
    }

    @Test
    void detailRejectsAnotherTeachersActivity() {
        when(activityService.getTeacherActivity(TEACHER_ID, ACTIVITY_ID))
                .thenThrow(new BusinessException("活动不存在或无权访问"));
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.detail(ACTIVITY_ID, session, new ConcurrentModel(), redirect);

        assertThat(view).isEqualTo("redirect:/teacher/activities");
        assertThat(redirect.getFlashAttributes().get("error")).isEqualTo("活动不存在或无权访问");
    }

    @Test
    void publishedActivityDoesNotOpenEditForm() {
        Activity published = activity(ActivityStatus.PUBLISHED);
        when(activityService.getTeacherActivity(TEACHER_ID, ACTIVITY_ID)).thenReturn(published);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.editForm(ACTIVITY_ID, session, new ConcurrentModel(), redirect);

        assertThat(view).isEqualTo("redirect:/teacher/activities/" + ACTIVITY_ID);
        assertThat(redirect.getFlashAttributes().get("error")).isEqualTo("只有草稿活动可以编辑");
    }

    @Test
    void updateDraftRedirectsToDetail() {
        ActivityForm form = validForm();

        String view = controller.update(
                ACTIVITY_ID, form, bindingResult(form), session,
                new ConcurrentModel(), new RedirectAttributesModelMap());

        verify(activityService).updateDraft(TEACHER_ID, ACTIVITY_ID, form);
        assertThat(view).isEqualTo("redirect:/teacher/activities/" + ACTIVITY_ID);
    }

    @Test
    void rejectedDeleteReturnsToDetailWithError() {
        doThrow(new BusinessException("只有草稿活动可以删除"))
                .when(activityService).deleteDraft(TEACHER_ID, ACTIVITY_ID);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.delete(ACTIVITY_ID, session, redirect);

        assertThat(view).isEqualTo("redirect:/teacher/activities/" + ACTIVITY_ID);
        assertThat(redirect.getFlashAttributes().get("error")).isEqualTo("只有草稿活动可以删除");
    }

    @Test
    void publishAndCloseUseLifecycleServiceMethods() {
        String publishView = controller.publish(ACTIVITY_ID, session, new RedirectAttributesModelMap());
        String closeView = controller.close(ACTIVITY_ID, session, new RedirectAttributesModelMap());

        verify(activityService).publish(TEACHER_ID, ACTIVITY_ID);
        verify(activityService).close(TEACHER_ID, ACTIVITY_ID);
        assertThat(publishView).isEqualTo("redirect:/teacher/activities/" + ACTIVITY_ID);
        assertThat(closeView).isEqualTo("redirect:/teacher/activities/" + ACTIVITY_ID);
    }

    private Activity activity(ActivityStatus status) {
        User teacher = new User("teacher", "hash", "测试教师", UserRole.TEACHER);
        ReflectionTestUtils.setField(teacher, "id", TEACHER_ID);
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

    private ActivityForm validForm() {
        ActivityForm form = new ActivityForm();
        form.setTitle("测试活动");
        form.setDescription("活动说明");
        form.setLocation("教学楼 A101");
        form.setRegistrationDeadline(LocalDateTime.of(2026, 10, 1, 12, 0));
        form.setStartTime(LocalDateTime.of(2026, 10, 2, 9, 0));
        form.setEndTime(LocalDateTime.of(2026, 10, 2, 11, 0));
        form.setCapacity(30);
        return form;
    }

    private BeanPropertyBindingResult bindingResult(ActivityForm form) {
        return new BeanPropertyBindingResult(form, "activityForm");
    }
}
