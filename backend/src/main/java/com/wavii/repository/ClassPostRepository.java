package com.wavii.repository;

import com.wavii.model.ClassPost;
import com.wavii.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassPostRepository extends JpaRepository<ClassPost, UUID> {
    List<ClassPost> findByTeacherOrderByCreatedAtDesc(User teacher);
}
