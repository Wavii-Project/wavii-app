package com.wavii.repository;

import com.wavii.model.DirectMessage;
import com.wavii.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    /**
     * Devuelve todos los mensajes directos entre dos usuarios, ordenados por fecha.
     */
    @Query("""
            SELECT dm FROM DirectMessage dm
            JOIN FETCH dm.sender
            JOIN FETCH dm.receiver
            WHERE (dm.sender = :userA AND dm.receiver = :userB)
               OR (dm.sender = :userB AND dm.receiver = :userA)
            ORDER BY dm.createdAt ASC
            """)
    List<DirectMessage> findConversation(@Param("userA") User userA, @Param("userB") User userB);
}
