package com.wavii.repository;

import com.wavii.model.AppNotification;
import com.wavii.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AppNotificationRepository extends JpaRepository<AppNotification, UUID> {
    List<AppNotification> findByRecipientOrderByCreatedAtDesc(User recipient);
    long countByRecipientAndReadFalse(User recipient);
    long deleteByRecipient(User recipient);

    @Modifying
    @Query("update AppNotification n set n.read = true where n.recipient = :recipient and n.read = false")
    int markAllReadByRecipient(@Param("recipient") User recipient);
}
