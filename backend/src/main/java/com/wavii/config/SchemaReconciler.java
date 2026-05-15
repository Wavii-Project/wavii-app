package com.wavii.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Reconciliador de esquema de base de datos.
 * Se encarga de ejecutar sentencias SQL de alteración para asegurar que la base de datos
 * tenga las columnas necesarias sin necesidad de migraciones manuales complejas.
 * 
 * @author eduglezexp
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SchemaReconciler {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    ApplicationRunner reconcileUserColumns() {
        return args -> {
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_cancel_at_period_end boolean NOT NULL DEFAULT false");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_current_period_end timestamp");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS deletion_scheduled_at timestamp");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS bio varchar(500)");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS instrument varchar(100)");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS city varchar(120)");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS address varchar(250)");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS province varchar(120)");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS contact_email varchar(160)");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS contact_phone varchar(30)");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS instagram_url varchar(300)");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS tiktok_url varchar(300)");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS youtube_url varchar(300)");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS facebook_url varchar(300)");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS banner_image_url varchar(300)");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS availability_preference varchar(20)");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS availability_notes varchar(500)");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS class_modality varchar(20)");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS price_per_hour numeric(10,2)");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS trial_used boolean NOT NULL DEFAULT false");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS best_streak integer NOT NULL DEFAULT 0");
            ensureColumn("ALTER TABLE users ADD COLUMN IF NOT EXISTS last_streak_date date");
            ensureColumn("ALTER TABLE pdf_documents ADD COLUMN IF NOT EXISTS description varchar(1000)");
            ensureColumn("ALTER TABLE pdf_documents ADD COLUMN IF NOT EXISTS cover_image_path varchar(255)");

            ensureColumn("ALTER TABLE forums ADD COLUMN IF NOT EXISTS like_count integer NOT NULL DEFAULT 0");
            ensureColumn("ALTER TABLE forum_memberships ADD COLUMN IF NOT EXISTS role varchar(20) NOT NULL DEFAULT 'MEMBER'");
            ensureColumn("ALTER TABLE band_listings ADD COLUMN IF NOT EXISTS cover_image_url varchar(500)");
            ensureColumn("UPDATE forum_memberships fm SET role='OWNER' FROM forums f WHERE fm.forum_id=f.id AND fm.user_id=f.creator_id AND fm.role='MEMBER'");

            ensureColumn("ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS teacher_name varchar(300)");
            ensureColumn("ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS student_name varchar(300)");
            ensureColumn("ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS instrument varchar(120)");
            ensureColumn("ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS city varchar(120)");
            ensureColumn("ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS province varchar(120)");
            ensureColumn("ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS modality varchar(20)");
            ensureColumn("ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS requested_modality varchar(20)");
            ensureColumn("ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS unit_price numeric(10,2)");
            ensureColumn("ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS payment_status varchar(30)");
            ensureColumn("ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS request_message varchar(1000)");
            ensureColumn("ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS request_availability varchar(500)");
            ensureColumn("ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS hours_purchased integer NOT NULL DEFAULT 1");
            ensureColumn("ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS hours_used integer NOT NULL DEFAULT 0");
            ensureColumn("ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS stripe_payment_intent_id varchar(120)");
            ensureColumn("ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS payment_receipt_number varchar(300)");
            ensureColumn("ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS class_link varchar(300)");
            ensureColumn("ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP");

            ensureColumn("ALTER TABLE class_sessions ADD COLUMN IF NOT EXISTS scheduled_at timestamp");
            ensureColumn("ALTER TABLE class_sessions ADD COLUMN IF NOT EXISTS duration_minutes integer NOT NULL DEFAULT 60");
            ensureColumn("ALTER TABLE class_sessions ADD COLUMN IF NOT EXISTS status varchar(30) NOT NULL DEFAULT 'scheduled'");
            ensureColumn("ALTER TABLE class_sessions ADD COLUMN IF NOT EXISTS meeting_url varchar(300)");
            ensureColumn("ALTER TABLE class_sessions ADD COLUMN IF NOT EXISTS notes varchar(500)");
            ensureColumn("ALTER TABLE class_sessions ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP");
            ensureColumn("ALTER TABLE class_sessions ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP");

            ensureColumn("ALTER TABLE class_messages ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP");
        };
    }

    private void ensureColumn(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ex) {
            log.warn("No se pudo reconciliar esquema con SQL: {}", sql, ex);
        }
    }
}
