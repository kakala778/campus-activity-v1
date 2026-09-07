package com.example.campusactivity.repository;

import com.example.campusactivity.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    boolean existsByStudentIdAndActivityId(Long studentId, Long activityId);

    long countByActivityId(Long activityId);

    List<Registration> findByStudentIdOrderByRegisteredAtDesc(Long studentId);

    List<Registration> findByActivityIdOrderByRegisteredAtAsc(Long activityId);

    Optional<Registration> findByStudentIdAndActivityId(Long studentId, Long activityId);
}
