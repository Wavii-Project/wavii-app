package com.wavii.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavii.model.AppNotification;
import com.wavii.model.User;
import com.wavii.repository.AppNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final AppNotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    /**
     * Crea y guarda una nueva notificación para un usuario.
     * 
     * @param recipient Usuario destinatario.
     * @param type Tipo de notificación (ej. "class_request").
     * @param title Título de la notificación.
     * @param body Cuerpo o mensaje de la notificación.
     * @param data Datos adicionales en formato mapa para adjuntar como JSON.
     * @return La notificación creada.
     */
    @Transactional
    public AppNotification create(User recipient, String type, String title, String body, Map<String, Object> data) {
        return notificationRepository.save(AppNotification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .body(body)
                .dataJson(writeJson(data))
                .build());
    }

    /**
     * Lista todas las notificaciones de un usuario, ordenadas por fecha descendente.
     * 
     * @param recipient Usuario que consulta sus notificaciones.
     * @return Lista de notificaciones en formato mapa.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(User recipient) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient).stream()
                .map(this::toMap)
                .toList();
    }

    /**
     * Marca una notificación específica como leída.
     * 
     * @param recipient Usuario dueño de la notificación.
     * @param notificationId ID de la notificación.
     * @return La notificación actualizada.
     */
    @Transactional
    public Map<String, Object> markRead(User recipient, UUID notificationId) {
        AppNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notificacion no encontrada"));
        if (!notification.getRecipient().getId().equals(recipient.getId())) {
            throw new IllegalArgumentException("No puedes modificar esta notificacion");
        }
        notification.setRead(true);
        return toMap(notificationRepository.save(notification));
    }

    /**
     * Obtiene un resumen de las notificaciones, como el conteo de no leídas.
     * 
     * @param recipient Usuario destinatario.
     * @return Mapa con el conteo de no leídas.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> summary(User recipient) {
        return Map.of("unreadCount", notificationRepository.countByRecipientAndReadFalse(recipient));
    }

    /**
     * Elimina todas las notificaciones de un usuario.
     * 
     * @param recipient Usuario destinatario.
     * @return Mapa con el número de notificaciones eliminadas.
     */
    @Transactional
    public Map<String, Object> clearAll(User recipient) {
        long removed = notificationRepository.deleteByRecipient(recipient);
        return Map.of("removed", removed);
    }

    /**
     * Marca todas las notificaciones de un usuario como leídas.
     * 
     * @param recipient Usuario destinatario.
     * @return Mapa con el número de notificaciones actualizadas.
     */
    @Transactional
    public Map<String, Object> markAllRead(User recipient) {
        int updated = notificationRepository.markAllReadByRecipient(recipient);
        return Map.of("updated", updated);
    }

    private Map<String, Object> toMap(AppNotification notification) {
        return Map.of(
                "id", notification.getId().toString(),
                "type", notification.getType(),
                "title", notification.getTitle(),
                "body", notification.getBody(),
                "data", readJson(notification.getDataJson()),
                "read", notification.isRead(),
                "createdAt", notification.getCreatedAt().toString()
        );
    }

    private String writeJson(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String dataJson) {
        if (dataJson == null || dataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(dataJson, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}
