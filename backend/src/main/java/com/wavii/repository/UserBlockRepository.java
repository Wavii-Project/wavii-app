package com.wavii.repository;

import com.wavii.model.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    Optional<UserBlock> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
}
