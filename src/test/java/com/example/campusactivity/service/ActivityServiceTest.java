package com.example.campusactivity.service;

import com.example.campusactivity.dto.ActivityForm;
import com.example.campusactivity.entity.Activity;
import com.example.campusactivity.entity.ActivityStatus;
import com.example.campusactivity.entity.User;
import com.example.campusactivity.entity.UserRole;
import com.example.campusactivity.exception.BusinessException;
import com.example.campusactivity.repository.ActivityRepository;
import com.example.campusactivity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    private static final Long TEACHER_ID = 10L;
    private static final Long OTHER_TEACHER_ID = 20L;
    private static final Long ACTIVITY_ID = 100L;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserRepository userRepository;

    private ActivityService activityService;
    private User teacher;

    @BeforeEach
    void setUp() {
        activityService = new ActivityService(activityRepository, userRepository);
        teacher = user(TEACHER_ID, UserRole.TEACHER);
    }

    @Test
    void teacherCanCreateDraftWithValidData() {
        when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Activity created = activityService.createDraft(TEACHER_ID, validForm("志愿活动"));

        assertThat(created.getStatus()).isEqualTo(ActivityStatus.DRAFT);
        assertThat(created.getCreator()).isSameAs(teacher);
        assertThat(created.getTitle()).isEqualTo("志愿活动");
    }

    @Test
    void studentCannotCreateDraft() {
        User student = user(30L, UserRole.STUDENT);
        when(userRepository.findById(30L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> activityService.createDraft(30L, validForm("违规活动")))
                .isInstanceOf(BusinessException.class);
        verify(activityRepository, never()).save(any());
    }

    @Test
    void teacherCanUpdateOwnDraft() {
        Activity draft = activity(teacher, ActivityStatus.DRAFT);
        when(activityRepository.findByIdAndCreatorId(ACTIVITY_ID, TEACHER_ID)).thenReturn(Optional.of(draft));
        when(activityRepository.save(draft)).thenReturn(draft);

        Activity updated = activityService.updateDraft(TEACHER_ID, ACTIVITY_ID, validForm("更新后的标题"));

        assertThat(updated.getTitle()).isEqualTo("更新后的标题");
        assertThat(updated.getStatus()).isEqualTo(ActivityStatus.DRAFT);
    }

    @Test
    void teacherCannotUpdatePublishedActivity() {
        Activity published = activity(teacher, ActivityStatus.PUBLISHED);
        when(activityRepository.findByIdAndCreatorId(ACTIVITY_ID, TEACHER_ID)).thenReturn(Optional.of(published));

        assertThatThrownBy(() -> activityService.updateDraft(
                TEACHER_ID, ACTIVITY_ID, validForm("不允许的修改")))
                .isInstanceOf(BusinessException.class);
        verify(activityRepository, never()).save(any());
    }

    @Test
    void teacherCannotUpdateAnotherTeachersDraft() {
        when(activityRepository.findByIdAndCreatorId(ACTIVITY_ID, OTHER_TEACHER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityService.updateDraft(
                OTHER_TEACHER_ID, ACTIVITY_ID, validForm("越权修改")))
                .isInstanceOf(BusinessException.class);
        verify(activityRepository, never()).save(any());
    }

    @Test
    void validDraftCanBePublished() {
        Activity draft = activity(teacher, ActivityStatus.DRAFT);
        when(activityRepository.findByIdAndCreatorId(ACTIVITY_ID, TEACHER_ID)).thenReturn(Optional.of(draft));
        when(activityRepository.save(draft)).thenReturn(draft);

        Activity published = activityService.publish(TEACHER_ID, ACTIVITY_ID);

        assertThat(published.getStatus()).isEqualTo(ActivityStatus.PUBLISHED);
    }

    @Test
    void publishRevalidatesCurrentActivityData() {
        Activity draft = activity(teacher, ActivityStatus.DRAFT);
        draft.setRegistrationDeadline(draft.getStartTime());
        when(activityRepository.findByIdAndCreatorId(ACTIVITY_ID, TEACHER_ID)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> activityService.publish(TEACHER_ID, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class);
        assertThat(draft.getStatus()).isEqualTo(ActivityStatus.DRAFT);
        verify(activityRepository, never()).save(any());
    }

    @Test
    void publishedActivityCannotBeDeleted() {
        Activity published = activity(teacher, ActivityStatus.PUBLISHED);
        when(activityRepository.findByIdAndCreatorId(ACTIVITY_ID, TEACHER_ID)).thenReturn(Optional.of(published));

        assertThatThrownBy(() -> activityService.deleteDraft(TEACHER_ID, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class);
        verify(activityRepository, never()).delete(any());
    }

    @Test
    void teacherCanDeleteOwnDraft() {
        Activity draft = activity(teacher, ActivityStatus.DRAFT);
        when(activityRepository.findByIdAndCreatorId(ACTIVITY_ID, TEACHER_ID)).thenReturn(Optional.of(draft));

        activityService.deleteDraft(TEACHER_ID, ACTIVITY_ID);

        verify(activityRepository).delete(draft);
    }

    @Test
    void teacherCanCloseOwnPublishedActivity() {
        Activity published = activity(teacher, ActivityStatus.PUBLISHED);
        when(activityRepository.findByIdAndCreatorId(ACTIVITY_ID, TEACHER_ID)).thenReturn(Optional.of(published));
        when(activityRepository.save(published)).thenReturn(published);

        Activity closed = activityService.close(TEACHER_ID, ACTIVITY_ID);

        assertThat(closed.getStatus()).isEqualTo(ActivityStatus.CLOSED);
    }

    @Test
    void closedActivityCannotBeClosedAgain() {
        Activity closed = activity(teacher, ActivityStatus.CLOSED);
        when(activityRepository.findByIdAndCreatorId(ACTIVITY_ID, TEACHER_ID)).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> activityService.close(TEACHER_ID, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class);
        verify(activityRepository, never()).save(any());
    }

    @Test
    void createRejectsBlankRequiredFields() {
        when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
        ActivityForm form = validForm("   ");

        assertThatThrownBy(() -> activityService.createDraft(TEACHER_ID, form))
                .isInstanceOf(BusinessException.class);
        verify(activityRepository, never()).save(any());
    }

    @Test
    void createRejectsNonPositiveCapacity() {
        when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
        ActivityForm form = validForm("容量无效");
        form.setCapacity(0);

        assertThatThrownBy(() -> activityService.createDraft(TEACHER_ID, form))
                .isInstanceOf(BusinessException.class);
        verify(activityRepository, never()).save(any());
    }

    @Test
    void createRejectsInvalidTimeOrder() {
        when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
        ActivityForm form = validForm("时间无效");
        form.setRegistrationDeadline(form.getStartTime());

        assertThatThrownBy(() -> activityService.createDraft(TEACHER_ID, form))
                .isInstanceOf(BusinessException.class);
        verify(activityRepository, never()).save(any());
    }

    @Test
    void teacherDetailLookupUsesTeacherAndActivityIdsTogether() {
        Activity ownActivity = activity(teacher, ActivityStatus.DRAFT);
        when(activityRepository.findByIdAndCreatorId(ACTIVITY_ID, TEACHER_ID))
                .thenReturn(Optional.of(ownActivity));

        Activity result = activityService.getTeacherActivity(TEACHER_ID, ACTIVITY_ID);

        assertThat(result).isSameAs(ownActivity);
    }

    @Test
    void teacherListContainsOnlyRepositoryResultsForThatTeacher() {
        Activity ownActivity = activity(teacher, ActivityStatus.DRAFT);
        when(activityRepository.findByCreatorIdOrderByCreatedAtDesc(TEACHER_ID))
                .thenReturn(List.of(ownActivity));

        assertThat(activityService.listTeacherActivities(TEACHER_ID)).containsExactly(ownActivity);
    }

    private ActivityForm validForm(String title) {
        ActivityForm form = new ActivityForm();
        form.setTitle(title);
        form.setDescription("活动说明");
        form.setLocation("教学楼 A101");
        form.setRegistrationDeadline(LocalDateTime.of(2026, 10, 1, 12, 0));
        form.setStartTime(LocalDateTime.of(2026, 10, 2, 9, 0));
        form.setEndTime(LocalDateTime.of(2026, 10, 2, 11, 0));
        form.setCapacity(30);
        return form;
    }

    private Activity activity(User creator, ActivityStatus status) {
        ActivityForm form = validForm("测试活动");
        Activity activity = new Activity(
                form.getTitle(), form.getDescription(), form.getLocation(),
                form.getRegistrationDeadline(), form.getStartTime(), form.getEndTime(),
                form.getCapacity(), creator
        );
        activity.setStatus(status);
        ReflectionTestUtils.setField(activity, "id", ACTIVITY_ID);
        return activity;
    }

    private User user(Long id, UserRole role) {
        User user = new User("user" + id, "hash", "测试用户", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
