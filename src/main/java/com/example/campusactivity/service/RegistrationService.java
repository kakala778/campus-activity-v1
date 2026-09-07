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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class RegistrationService {

    private final UserService userService;
    private final ActivityRepository activityRepository;
    private final RegistrationRepository registrationRepository;

    public RegistrationService(UserService userService, ActivityRepository activityRepository,
                               RegistrationRepository registrationRepository) {
        this.userService = userService;
        this.activityRepository = activityRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional
    public Registration register(Long studentId, Long activityId) {
        User student = userService.getRequiredUser(studentId);
        if (student.getRole() != UserRole.STUDENT) {
            throw new BusinessException("只有学生可以报名活动");
        }

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException("活动不存在"));
        if (activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new BusinessException("只有已发布活动可以报名");
        }
        if (LocalDateTime.now().isAfter(activity.getRegistrationDeadline())) {
            throw new BusinessException("活动报名已截止");
        }
        if (registrationRepository.existsByStudentIdAndActivityId(studentId, activityId)) {
            throw new BusinessException("不能重复报名同一活动");
        }
        if (registrationRepository.countByActivityId(activityId) >= activity.getCapacity()) {
            throw new BusinessException("活动报名人数已满");
        }

        return registrationRepository.save(new Registration(student, activity));
    }

    public RegistrationStatusView getStatus(Long studentId, Activity activity) {
        long registrationCount = registrationRepository.countByActivityId(activity.getId());
        boolean alreadyRegistered = registrationRepository
                .existsByStudentIdAndActivityId(studentId, activity.getId());
        boolean deadlinePassed = LocalDateTime.now().isAfter(activity.getRegistrationDeadline());
        boolean full = registrationCount >= activity.getCapacity();
        return new RegistrationStatusView(
                registrationCount, alreadyRegistered, deadlinePassed, full
        );
    }
}
