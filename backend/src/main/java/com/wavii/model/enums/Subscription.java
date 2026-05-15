package com.wavii.model.enums;

/**
 * Enum que define los planes de suscripcion disponibles.
 */
public enum Subscription {
    /** Plan gratuito con funcionalidades limitadas */
    FREE,
    /** Plan de pago con acceso completo a tablaturas y desafios */
    PLUS,
    /** Plan educativo para instituciones y escuelas */
    SCHOLAR;

    public static Subscription fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return FREE;
        }

        if ("EDUCATION".equalsIgnoreCase(value) || "SCHOLAR".equalsIgnoreCase(value)) {
            return SCHOLAR;
        }

        return Subscription.valueOf(value.toUpperCase());
    }

    public String toDatabaseValue() {
        return this == SCHOLAR ? "EDUCATION" : name();
    }

    public String toPublicId() {
        return this == SCHOLAR ? "education" : name().toLowerCase();
    }
}
