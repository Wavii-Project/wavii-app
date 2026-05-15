package com.wavii.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchemaReconcilerTest {

    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks private SchemaReconciler schemaReconciler;

    @Test
    void reconcileUserColumnsCreatesApplicationRunnerTest() throws Exception {
        ApplicationRunner runner = schemaReconciler.reconcileUserColumns();
        assertNotNull(runner);
    }

    @Test
    void reconcileUserColumnsExecutesAllAlterStatementsTest() throws Exception {
        doNothing().when(jdbcTemplate).execute(anyString());

        ApplicationRunner runner = schemaReconciler.reconcileUserColumns();
        runner.run(null);

        verify(jdbcTemplate, times(53)).execute(anyString());
    }

    @Test
    void reconcileUserColumnsExecutesSubscriptionCancelColumnTest() throws Exception {
        doNothing().when(jdbcTemplate).execute(anyString());

        ApplicationRunner runner = schemaReconciler.reconcileUserColumns();
        runner.run(null);

        verify(jdbcTemplate).execute(
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_cancel_at_period_end boolean NOT NULL DEFAULT false");
    }

    @Test
    void reconcileUserColumnsExecutesBioColumnTest() throws Exception {
        doNothing().when(jdbcTemplate).execute(anyString());

        ApplicationRunner runner = schemaReconciler.reconcileUserColumns();
        runner.run(null);

        verify(jdbcTemplate).execute(
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS bio varchar(500)");
    }

    @Test
    void reconcileUserColumnsSqlExceptionDoesNotPropagateTest() throws Exception {
        doThrow(new RuntimeException("SQL error")).when(jdbcTemplate).execute(anyString());

        ApplicationRunner runner = schemaReconciler.reconcileUserColumns();
        assertDoesNotThrow(() -> runner.run(null));
    }

    @Test
    void reconcileUserColumnsSomeColumnsFailContinuesWithRestTest() throws Exception {
        doNothing().when(jdbcTemplate).execute(anyString());
        doThrow(new RuntimeException("Column already exists"))
                .when(jdbcTemplate).execute(contains("subscription_cancel_at_period_end"));

        ApplicationRunner runner = schemaReconciler.reconcileUserColumns();
        assertDoesNotThrow(() -> runner.run(null));

        verify(jdbcTemplate, atLeast(1)).execute(anyString());
    }
}
