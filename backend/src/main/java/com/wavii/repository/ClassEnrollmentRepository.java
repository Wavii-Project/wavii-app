package com.wavii.repository;

import com.wavii.model.ClassEnrollment;
import com.wavii.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, UUID> {
    List<ClassEnrollment> findByTeacherOrStudentOrderByCreatedAtDesc(User teacher, User student);
    List<ClassEnrollment> findByTeacherOrderByCreatedAtDesc(User teacher);
    List<ClassEnrollment> findByStudentOrderByCreatedAtDesc(User student);
    Optional<ClassEnrollment> findFirstByTeacherAndStudentOrderByCreatedAtDesc(User teacher, User student);
    boolean existsByTeacherAndStudent(User teacher, User student);
    boolean existsByTeacherAndStudentAndPaymentStatusIgnoreCase(User teacher, User student, String paymentStatus);
}
