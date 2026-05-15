package com.wavii.repository;

import com.wavii.model.User;
import com.wavii.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByToken(String token);

    List<VerificationToken> findAllByUserAndTypeAndUsedFalse(User user, String type);
}
