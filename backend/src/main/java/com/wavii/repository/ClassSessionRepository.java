package com.wavii.repository;

import com.wavii.model.ClassEnrollment;
import com.wavii.model.ClassSession;
import com.wavii.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID> {
    List<ClassSession> findByEnrollmentOrderByScheduledAtAsc(ClassEnrollment enrollment);
    Optional<ClassSession> findFirstByEnrollmentAndStatusOrderByScheduledAtAsc(ClassEnrollment enrollment, String status);
    List<ClassSession> findByTeacherOrderByScheduledAtAsc(User teacher);
}
