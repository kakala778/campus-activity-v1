package com.example.campusactivity.service;

import com.example.campusactivity.dto.ActivityForm;
import com.example.campusactivity.entity.Activity;
import com.example.campusactivity.entity.ActivityStatus;
import com.example.campusactivity.entity.User;
import com.example.campusactivity.entity.UserRole;
import com.example.campusactivity.exception.BusinessException;
import com.example.campusactivity.repository.ActivityRepository;
import com.example.campusactivity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    public ActivityService(ActivityRepository activityRepository, UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Activity createDraft(Long teacherId, ActivityForm form) {
        User teacher = requireTeacher(teacherId);
        validateForm(form);
        Activity activity = new Activity(
                form.getTitle().trim(),
                form.getDescription().trim(),
                form.getLocation().trim(),
                form.getRegistrationDeadline(),
                form.getStartTime(),
                form.getEndTime(),
                form.getCapacity(),
                teacher
        );
        activity.setStatus(ActivityStatus.DRAFT);
        return activityRepository.save(activity);
    }

    @Transactional
    public Activity updateDraft(Long teacherId, Long activityId, ActivityForm form) {
        Activity activity = getOwnedActivity(teacherId, activityId);
        requireStatus(activity, ActivityStatus.DRAFT, "只有草稿活动可以编辑");
        validateForm(form);
        applyForm(activity, form);
        return activityRepository.save(activity);
    }

    @Transactional
    public void deleteDraft(Long teacherId, Long activityId) {
        Activity activity = getOwnedActivity(teacherId, activityId);
        requireStatus(activity, ActivityStatus.DRAFT, "只有草稿活动可以删除");
        activityRepository.delete(activity);
    }

    @Transactional
    public Activity publish(Long teacherId, Long activityId) {
        Activity activity = getOwnedActivity(teacherId, activityId);
        requireStatus(activity, ActivityStatus.DRAFT, "只有草稿活动可以发布");
        validateData(
                activity.getTitle(), activity.getDescription(), activity.getLocation(),
                activity.getRegistrationDeadline(), activity.getStartTime(), activity.getEndTime(),
                activity.getCapacity()
        );
        activity.setStatus(ActivityStatus.PUBLISHED);
        return activityRepository.save(activity);
    }

    @Transactional
    public Activity close(Long teacherId, Long activityId) {
        Activity activity = getOwnedActivity(teacherId, activityId);
        requireStatus(activity, ActivityStatus.PUBLISHED, "只有已发布活动可以关闭");
        activity.setStatus(ActivityStatus.CLOSED);
        return activityRepository.save(activity);
    }

    public List<Activity> listTeacherActivities(Long teacherId) {
        return activityRepository.findByCreatorIdOrderByCreatedAtDesc(teacherId);
    }

    public Activity getTeacherActivity(Long teacherId, Long activityId) {
        return getOwnedActivity(teacherId, activityId);
    }

    public List<Activity> listPublishedActivities() {
        return activityRepository.findByStatusOrderByStartTimeAsc(ActivityStatus.PUBLISHED);
    }

    public Activity getPublishedActivity(Long activityId) {
        return activityRepository.findByIdAndStatus(activityId, ActivityStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException("活动不存在或未发布"));
    }

    private User requireTeacher(Long teacherId) {
        User user = userRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (user.getRole() != UserRole.TEACHER) {
            throw new BusinessException("只有教师可以创建活动");
        }
        return user;
    }

    private Activity getOwnedActivity(Long teacherId, Long activityId) {
        return activityRepository.findByIdAndCreatorId(activityId, teacherId)
                .orElseThrow(() -> new BusinessException("活动不存在或无权访问"));
    }

    private void requireStatus(Activity activity, ActivityStatus expected, String message) {
        if (activity.getStatus() != expected) {
            throw new BusinessException(message);
        }
    }

    private void validateForm(ActivityForm form) {
        if (form == null) {
            throw new BusinessException("活动信息不能为空");
        }
        validateData(
                form.getTitle(), form.getDescription(), form.getLocation(),
                form.getRegistrationDeadline(), form.getStartTime(), form.getEndTime(),
                form.getCapacity()
        );
    }

    private void validateData(String title, String description, String location,
                              LocalDateTime registrationDeadline, LocalDateTime startTime,
                              LocalDateTime endTime, Integer capacity) {
        if (isBlank(title) || isBlank(description) || isBlank(location)) {
            throw new BusinessException("标题、说明和地点不能为空");
        }
        if (capacity == null || capacity <= 0) {
            throw new BusinessException("活动容量必须大于 0");
        }
        if (registrationDeadline == null || startTime == null || endTime == null
                || !registrationDeadline.isBefore(startTime) || !startTime.isBefore(endTime)) {
            throw new BusinessException("时间必须满足报名截止时间早于开始时间，开始时间早于结束时间");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void applyForm(Activity activity, ActivityForm form) {
        activity.setTitle(form.getTitle().trim());
        activity.setDescription(form.getDescription().trim());
        activity.setLocation(form.getLocation().trim());
        activity.setRegistrationDeadline(form.getRegistrationDeadline());
        activity.setStartTime(form.getStartTime());
        activity.setEndTime(form.getEndTime());
        activity.setCapacity(form.getCapacity());
    }
}
