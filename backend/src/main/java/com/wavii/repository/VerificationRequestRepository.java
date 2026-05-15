package com.wavii.repository;

import com.wavii.model.User;
import com.wavii.model.VerificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, UUID> {

    Optional<VerificationRequest> findTopByUserOrderByCreatedAtDesc(User user);
}
