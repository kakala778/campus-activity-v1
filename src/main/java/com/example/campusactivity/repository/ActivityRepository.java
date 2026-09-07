package com.example.campusactivity.repository;

import com.example.campusactivity.entity.Activity;
import com.example.campusactivity.entity.ActivityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByStatusOrderByStartTimeAsc(ActivityStatus status);

    Optional<Activity> findByIdAndStatus(Long id, ActivityStatus status);

    List<Activity> findByCreatorIdOrderByCreatedAtDesc(Long creatorId);

    Optional<Activity> findByIdAndCreatorId(Long id, Long creatorId);
}
