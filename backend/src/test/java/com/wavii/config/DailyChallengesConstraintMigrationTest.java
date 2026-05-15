package com.wavii.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyChallengesConstraintMigrationTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DailyChallengesConstraintMigration migration;

    @Test
    void runWithNoConstraintsCreatesExpectedConstraintTest() throws Exception {
        when(jdbcTemplate.queryForList(anyString(), eq("daily_challenges"))).thenReturn(List.of());

        migration.run();

        verify(jdbcTemplate).execute(contains("ADD CONSTRAINT uk_daily_challenges_date_difficulty_slot"));
    }

    @Test
    void runWithExpectedConstraintAlreadyExistsDoesNotAddAgainTest() throws Exception {
        Map<String, Object> existingConstraint = Map.of(
                "constraint_name", "uk_daily_challenges_date_difficulty_slot",
                "cols", "challenge_date,difficulty,slot"
        );
        when(jdbcTemplate.queryForList(anyString(), eq("daily_challenges")))
                .thenReturn(List.of(existingConstraint));

        migration.run();

        verify(jdbcTemplate, never()).execute(contains("ADD CONSTRAINT"));
    }

    @Test
    void runWithOldConstraintDropsItTest() throws Exception {
        Map<String, Object> oldConstraint = Map.of(
                "constraint_name", "daily_challenges_challenge_date_difficulty_key",
                "cols", "challenge_date,difficulty"
        );
        when(jdbcTemplate.queryForList(anyString(), eq("daily_challenges")))
                .thenReturn(List.of(oldConstraint));

        migration.run();

        verify(jdbcTemplate).execute(contains("DROP CONSTRAINT daily_challenges_challenge_date_difficulty_key"));
        verify(jdbcTemplate).execute(contains("ADD CONSTRAINT uk_daily_challenges_date_difficulty_slot"));
    }

    @Test
    void runWithOldAndNewConstraintDropsOldAndKeepsNewTest() throws Exception {
        Map<String, Object> oldConstraint = Map.of(
                "constraint_name", "old_constraint",
                "cols", "challenge_date,difficulty"
        );
        Map<String, Object> newConstraint = Map.of(
                "constraint_name", "uk_daily_challenges_date_difficulty_slot",
                "cols", "challenge_date,difficulty,slot"
        );
        when(jdbcTemplate.queryForList(anyString(), eq("daily_challenges")))
                .thenReturn(List.of(oldConstraint, newConstraint));

        migration.run();

        verify(jdbcTemplate).execute(contains("DROP CONSTRAINT old_constraint"));
        verify(jdbcTemplate, never()).execute(contains("ADD CONSTRAINT"));
    }

    @Test
    void runWithUnrelatedConstraintIgnoresItTest() throws Exception {
        Map<String, Object> unrelated = Map.of(
                "constraint_name", "some_other_constraint",
                "cols", "other_column"
        );
        when(jdbcTemplate.queryForList(anyString(), eq("daily_challenges")))
                .thenReturn(List.of(unrelated));

        migration.run();

        verify(jdbcTemplate, never()).execute(contains("DROP CONSTRAINT some_other_constraint"));
        verify(jdbcTemplate).execute(contains("ADD CONSTRAINT uk_daily_challenges_date_difficulty_slot"));
    }
}
