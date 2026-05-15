package com.wavii.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Corrige constraints antiguos de daily_challenges en despliegues ya existentes:
 * - elimina unique(challenge_date, difficulty)
 * - garantiza unique(challenge_date, difficulty, slot)
 * 
 * @author eduglezexp
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyChallengesConstraintMigration implements CommandLineRunner {

    private static final String TABLE_NAME = "daily_challenges";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        List<Map<String, Object>> constraints = jdbcTemplate.queryForList("""
                SELECT tc.constraint_name,
                       string_agg(kcu.column_name, ',' ORDER BY kcu.ordinal_position) AS cols
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_name = kcu.constraint_name
                 AND tc.table_schema = kcu.table_schema
                WHERE tc.table_schema = 'public'
                  AND tc.table_name = ?
                  AND tc.constraint_type = 'UNIQUE'
                GROUP BY tc.constraint_name
                """, TABLE_NAME);

        boolean hasExpectedConstraint = false;

        for (Map<String, Object> row : constraints) {
            String constraintName = String.valueOf(row.get("constraint_name"));
            String cols = String.valueOf(row.get("cols"));

            if ("challenge_date,difficulty,slot".equals(cols)) {
                hasExpectedConstraint = true;
                continue;
            }

            if ("challenge_date,difficulty".equals(cols)) {
                jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME + " DROP CONSTRAINT " + constraintName);
                log.info("Constraint antiguo eliminado en {}: {}", TABLE_NAME, constraintName);
            }
        }

        if (!hasExpectedConstraint) {
            jdbcTemplate.execute("""
                    ALTER TABLE daily_challenges
                    ADD CONSTRAINT uk_daily_challenges_date_difficulty_slot
                    UNIQUE (challenge_date, difficulty, slot)
                    """);
            log.info("Constraint nuevo creado en {}: unique(challenge_date, difficulty, slot)", TABLE_NAME);
        }
    }
}
