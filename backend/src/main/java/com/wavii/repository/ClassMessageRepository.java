package com.wavii.repository;

import com.wavii.model.ClassEnrollment;
import com.wavii.model.ClassMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassMessageRepository extends JpaRepository<ClassMessage, UUID> {
    List<ClassMessage> findByEnrollmentOrderByCreatedAtAsc(ClassEnrollment enrollment);
}
