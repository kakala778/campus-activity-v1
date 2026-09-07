package com.example.campusactivity.service;

import com.example.campusactivity.dto.RegistrationStatusView;
import com.example.campusactivity.entity.Activity;
import com.example.campusactivity.entity.ActivityStatus;
import com.example.campusactivity.entity.Registration;
import com.example.campusactivity.entity.User;
import com.example.campusactivity.entity.UserRole;
import com.example.campusactivity.exception.BusinessException;
import com.example.campusactivity.repository.ActivityRepository;
import com.example.campusactivity.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    private static final Long STUDENT_ID = 30L;
    private static final Long ACTIVITY_ID = 100L;

    @Mock
    private UserService userService;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    private RegistrationService registrationService;
    private User student;
    private Activity activity;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(userService, activityRepository, registrationRepository);
        student = user(STUDENT_ID, UserRole.STUDENT);
        activity = activity(ActivityStatus.PUBLISHED, 30, LocalDateTime.now().plusDays(1));
    }

    @Test
    void validStudentRegistersForPublishedAvailableActivity() {
        arrangeValidRegistration();
        when(registrationRepository.save(any(Registration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Registration registration = registrationService.register(STUDENT_ID, ACTIVITY_ID);

        assertThat(registration.getStudent()).isSameAs(student);
        assertThat(registration.getActivity()).isSameAs(activity);
        InOrder order = inOrder(userService, activityRepository, registrationRepository);
        order.verify(userService).getRequiredUser(STUDENT_ID);
        order.verify(activityRepository).findById(ACTIVITY_ID);
        order.verify(registrationRepository).existsByStudentIdAndActivityId(STUDENT_ID, ACTIVITY_ID);
        order.verify(registrationRepository).countByActivityId(ACTIVITY_ID);
        order.verify(registrationRepository).save(any(Registration.class));
    }

    @Test
    void duplicateRegistrationIsRejectedWithoutSaving() {
        when(userService.getRequiredUser(STUDENT_ID)).thenReturn(student);
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(registrationRepository.existsByStudentIdAndActivityId(STUDENT_ID, ACTIVITY_ID)).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(STUDENT_ID, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不能重复报名同一活动");
        verify(registrationRepository, never()).save(any());
        verify(registrationRepository, never()).countByActivityId(any());
    }

    @Test
    void fullActivityIsRejectedWithoutSaving() {
        when(userService.getRequiredUser(STUDENT_ID)).thenReturn(student);
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(registrationRepository.existsByStudentIdAndActivityId(STUDENT_ID, ACTIVITY_ID)).thenReturn(false);
        when(registrationRepository.countByActivityId(ACTIVITY_ID)).thenReturn(30L);

        assertThatThrownBy(() -> registrationService.register(STUDENT_ID, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("活动报名人数已满");
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void registrationAfterDeadlineIsRejectedWithoutSaving() {
        activity = activity(ActivityStatus.PUBLISHED, 30, LocalDateTime.now().minusMinutes(1));
        when(userService.getRequiredUser(STUDENT_ID)).thenReturn(student);
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));

        assertThatThrownBy(() -> registrationService.register(STUDENT_ID, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("活动报名已截止");
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void closedActivityIsRejectedWithoutSaving() {
        assertStatusRejected(ActivityStatus.CLOSED);
    }

    @Test
    void draftActivityIsRejectedWithoutSaving() {
        assertStatusRejected(ActivityStatus.DRAFT);
    }

    @Test
    void teacherCannotRegisterAsStudent() {
        User teacher = user(10L, UserRole.TEACHER);
        when(userService.getRequiredUser(10L)).thenReturn(teacher);

        assertThatThrownBy(() -> registrationService.register(10L, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有学生可以报名活动");
        verify(activityRepository, never()).findById(any());
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void missingActivityIsRejectedWithoutSaving() {
        when(userService.getRequiredUser(STUDENT_ID)).thenReturn(student);
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.register(STUDENT_ID, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("活动不存在");
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void availableActivityStatusShowsCountAndAllowsRegistration() {
        when(registrationRepository.countByActivityId(ACTIVITY_ID)).thenReturn(3L);
        when(registrationRepository.existsByStudentIdAndActivityId(STUDENT_ID, ACTIVITY_ID)).thenReturn(false);

        RegistrationStatusView status = registrationService.getStatus(STUDENT_ID, activity);

        assertThat(status.registrationCount()).isEqualTo(3L);
        assertThat(status.alreadyRegistered()).isFalse();
        assertThat(status.deadlinePassed()).isFalse();
        assertThat(status.full()).isFalse();
        assertThat(status.canRegister()).isTrue();
    }

    @Test
    void statusShowsAlreadyRegisteredAsUnavailable() {
        when(registrationRepository.countByActivityId(ACTIVITY_ID)).thenReturn(1L);
        when(registrationRepository.existsByStudentIdAndActivityId(STUDENT_ID, ACTIVITY_ID)).thenReturn(true);

        RegistrationStatusView status = registrationService.getStatus(STUDENT_ID, activity);

        assertThat(status.alreadyRegistered()).isTrue();
        assertThat(status.canRegister()).isFalse();
    }

    @Test
    void statusShowsDeadlinePassedAsUnavailable() {
        activity = activity(ActivityStatus.PUBLISHED, 30, LocalDateTime.now().minusMinutes(1));
        when(registrationRepository.countByActivityId(ACTIVITY_ID)).thenReturn(0L);
        when(registrationRepository.existsByStudentIdAndActivityId(STUDENT_ID, ACTIVITY_ID)).thenReturn(false);

        RegistrationStatusView status = registrationService.getStatus(STUDENT_ID, activity);

        assertThat(status.deadlinePassed()).isTrue();
        assertThat(status.canRegister()).isFalse();
    }

    @Test
    void statusShowsFullActivityAsUnavailable() {
        activity = activity(ActivityStatus.PUBLISHED, 1, LocalDateTime.now().plusDays(1));
        when(registrationRepository.countByActivityId(ACTIVITY_ID)).thenReturn(1L);
        when(registrationRepository.existsByStudentIdAndActivityId(STUDENT_ID, ACTIVITY_ID)).thenReturn(false);

        RegistrationStatusView status = registrationService.getStatus(STUDENT_ID, activity);

        assertThat(status.full()).isTrue();
        assertThat(status.canRegister()).isFalse();
    }

    @Test
    void studentListContainsOnlyOwnRegistrationsInRepositoryOrder() {
        Registration newest = registration(student, activity, LocalDateTime.now());
        Registration older = registration(student, activity, LocalDateTime.now().minusHours(1));
        when(registrationRepository.findByStudentIdOrderByRegisteredAtDesc(STUDENT_ID))
                .thenReturn(List.of(newest, older));

        assertThat(registrationService.listStudentRegistrations(STUDENT_ID))
                .containsExactly(newest, older);
    }

    @Test
    void closedActivityRegistrationRemainsInStudentList() {
        activity.setStatus(ActivityStatus.CLOSED);
        Registration closedRegistration = registration(student, activity, LocalDateTime.now());
        when(registrationRepository.findByStudentIdOrderByRegisteredAtDesc(STUDENT_ID))
                .thenReturn(List.of(closedRegistration));

        assertThat(registrationService.listStudentRegistrations(STUDENT_ID))
                .containsExactly(closedRegistration);
    }

    @Test
    void studentCanCancelOwnPublishedRegistrationBeforeDeadline() {
        Registration registration = registration(student, activity, LocalDateTime.now());
        when(registrationRepository.findByStudentIdAndActivityId(STUDENT_ID, ACTIVITY_ID))
                .thenReturn(Optional.of(registration));

        registrationService.cancel(STUDENT_ID, ACTIVITY_ID);

        verify(registrationRepository).delete(registration);
    }

    @Test
    void studentCanRegisterAgainAfterCancellation() {
        Registration existing = registration(student, activity, LocalDateTime.now());
        AtomicBoolean registered = new AtomicBoolean(true);
        when(registrationRepository.findByStudentIdAndActivityId(STUDENT_ID, ACTIVITY_ID))
                .thenReturn(Optional.of(existing));
        doAnswer(invocation -> {
            registered.set(false);
            return null;
        }).when(registrationRepository).delete(existing);
        when(userService.getRequiredUser(STUDENT_ID)).thenReturn(student);
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(registrationRepository.existsByStudentIdAndActivityId(STUDENT_ID, ACTIVITY_ID))
                .thenAnswer(invocation -> registered.get());
        when(registrationRepository.countByActivityId(ACTIVITY_ID)).thenReturn(0L);
        when(registrationRepository.save(any(Registration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        registrationService.cancel(STUDENT_ID, ACTIVITY_ID);
        Registration newRegistration = registrationService.register(STUDENT_ID, ACTIVITY_ID);

        assertThat(newRegistration.getStudent()).isSameAs(student);
        assertThat(newRegistration.getActivity()).isSameAs(activity);
    }

    @Test
    void cancellationAfterDeadlineIsRejectedWithoutDeleting() {
        activity = activity(ActivityStatus.PUBLISHED, 30, LocalDateTime.now().minusMinutes(1));
        Registration registration = registration(student, activity, LocalDateTime.now().minusDays(1));
        when(registrationRepository.findByStudentIdAndActivityId(STUDENT_ID, ACTIVITY_ID))
                .thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> registrationService.cancel(STUDENT_ID, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("报名截止后不能取消报名");
        verify(registrationRepository, never()).delete(any());
    }

    @Test
    void closedActivityRegistrationCannotBeCancelled() {
        activity.setStatus(ActivityStatus.CLOSED);
        Registration registration = registration(student, activity, LocalDateTime.now());
        when(registrationRepository.findByStudentIdAndActivityId(STUDENT_ID, ACTIVITY_ID))
                .thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> registrationService.cancel(STUDENT_ID, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有已发布活动可以取消报名");
        verify(registrationRepository, never()).delete(any());
    }

    @Test
    void studentCannotCancelAnotherStudentsOrMissingRegistration() {
        when(registrationRepository.findByStudentIdAndActivityId(STUDENT_ID, ACTIVITY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.cancel(STUDENT_ID, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("报名记录不存在或无权取消");
        verify(registrationRepository, never()).delete(any());
    }

    @Test
    void teacherCanReadOwnActivityRegistrationListAndCount() {
        User teacher = activity.getCreator();
        Registration registration = registration(student, activity, LocalDateTime.now());
        when(activityRepository.findByIdAndCreatorId(ACTIVITY_ID, teacher.getId()))
                .thenReturn(Optional.of(activity));
        when(registrationRepository.findByActivityIdOrderByRegisteredAtAsc(ACTIVITY_ID))
                .thenReturn(List.of(registration));
        when(registrationRepository.countByActivityId(ACTIVITY_ID)).thenReturn(1L);

        assertThat(registrationService.listActivityRegistrations(teacher.getId(), ACTIVITY_ID))
                .containsExactly(registration);
        assertThat(registrationService.countActivityRegistrations(teacher.getId(), ACTIVITY_ID))
                .isEqualTo(1L);
    }

    @Test
    void anotherTeacherCannotReadRegistrationListOrCount() {
        when(activityRepository.findByIdAndCreatorId(ACTIVITY_ID, 20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.listActivityRegistrations(20L, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("活动不存在或无权访问");
        assertThatThrownBy(() -> registrationService.countActivityRegistrations(20L, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("活动不存在或无权访问");
        verify(registrationRepository, never()).findByActivityIdOrderByRegisteredAtAsc(any());
        verify(registrationRepository, never()).countByActivityId(any());
    }

    @Test
    void teacherCanStillReadRegistrationsAfterActivityCloses() {
        activity.setStatus(ActivityStatus.CLOSED);
        User teacher = activity.getCreator();
        Registration registration = registration(student, activity, LocalDateTime.now());
        when(activityRepository.findByIdAndCreatorId(ACTIVITY_ID, teacher.getId()))
                .thenReturn(Optional.of(activity));
        when(registrationRepository.findByActivityIdOrderByRegisteredAtAsc(ACTIVITY_ID))
                .thenReturn(List.of(registration));

        assertThat(registrationService.listActivityRegistrations(teacher.getId(), ACTIVITY_ID))
                .containsExactly(registration);
    }

    @Test
    void publishedRegistrationBeforeDeadlineCanBeCancelled() {
        Registration registration = registration(student, activity, LocalDateTime.now());

        assertThat(registrationService.canCancel(registration)).isTrue();
    }

    @Test
    void closedRegistrationIsShownAsNotCancellable() {
        activity.setStatus(ActivityStatus.CLOSED);
        Registration registration = registration(student, activity, LocalDateTime.now());

        assertThat(registrationService.canCancel(registration)).isFalse();
    }

    @Test
    void expiredRegistrationIsShownAsNotCancellable() {
        activity = activity(ActivityStatus.PUBLISHED, 30, LocalDateTime.now().minusMinutes(1));
        Registration registration = registration(student, activity, LocalDateTime.now().minusDays(1));

        assertThat(registrationService.canCancel(registration)).isFalse();
    }

    private void arrangeValidRegistration() {
        when(userService.getRequiredUser(STUDENT_ID)).thenReturn(student);
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
        when(registrationRepository.existsByStudentIdAndActivityId(STUDENT_ID, ACTIVITY_ID)).thenReturn(false);
        when(registrationRepository.countByActivityId(ACTIVITY_ID)).thenReturn(0L);
    }

    private void assertStatusRejected(ActivityStatus status) {
        activity.setStatus(status);
        when(userService.getRequiredUser(STUDENT_ID)).thenReturn(student);
        when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));

        assertThatThrownBy(() -> registrationService.register(STUDENT_ID, ACTIVITY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有已发布活动可以报名");
        verify(registrationRepository, never()).save(any());
    }

    private User user(Long id, UserRole role) {
        User user = new User("user" + id, "hash", "测试用户", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Activity activity(ActivityStatus status, int capacity, LocalDateTime deadline) {
        User teacher = user(10L, UserRole.TEACHER);
        Activity activity = new Activity(
                "测试活动", "活动说明", "教学楼 A101",
                deadline, deadline.plusDays(1), deadline.plusDays(1).plusHours(2),
                capacity, teacher
        );
        activity.setStatus(status);
        ReflectionTestUtils.setField(activity, "id", ACTIVITY_ID);
        return activity;
    }

    private Registration registration(User owner, Activity registeredActivity, LocalDateTime registeredAt) {
        Registration registration = new Registration(owner, registeredActivity);
        ReflectionTestUtils.setField(registration, "registeredAt", registeredAt);
        return registration;
    }
}
