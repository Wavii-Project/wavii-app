package com.wavii.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa un mensaje directo entre dos usuarios.
 */
@Entity
@Table(name = "direct_messages", indexes = {
        @Index(name = "idx_dm_sender", columnList = "sender_id"),
        @Index(name = "idx_dm_receiver", columnList = "receiver_id"),
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(length = 2000, nullable = false)
    private String content;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
