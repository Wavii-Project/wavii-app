package com.wavii.controller;

import com.wavii.dto.challenge.CompleteChallengResponseDto;
import com.wavii.dto.challenge.DailyChallengeDto;
import com.wavii.dto.challenge.StatsDto;
import com.wavii.model.User;
import com.wavii.model.enums.Level;
import com.wavii.service.DailyChallengeService;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyChallengeControllerTest {

    @Mock private DailyChallengeService challengeService;

    @InjectMocks private DailyChallengeController controller;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setLevel(Level.PRINCIPIANTE);
    }

    // ─── getTodayChallenges ───────────────────────────────────────

    @Test
    void getTodayChallengesReturnsChallengeListTest() {
        DailyChallengeDto dto = new DailyChallengeDto(
                1L, LocalDate.now(), Level.PRINCIPIANTE, 15, 1L, "Song", "Owner", null, null, false);
        when(challengeService.getTodayChallengesForUser(user)).thenReturn(List.of(dto));

        ResponseEntity<List<DailyChallengeDto>> result = controller.getTodayChallenges(user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void getTodayChallengesEmptyListReturnsOkTest() {
        when(challengeService.getTodayChallengesForUser(user)).thenReturn(List.of());

        ResponseEntity<List<DailyChallengeDto>> result = controller.getTodayChallenges(user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    // ─── completeChallenge ────────────────────────────────────────

    @Test
    void completeChallengeSuccessReturnsOkTest() {
        CompleteChallengResponseDto response = new CompleteChallengResponseDto(
                15, 15, 1, false, 1, 1);
        when(challengeService.completeChallenge(1L, user)).thenReturn(response);

        ResponseEntity<CompleteChallengResponseDto> result = controller.completeChallenge(1L, user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void completeChallengeNotFoundReturns404Test() {
        when(challengeService.completeChallenge(99L, user))
                .thenThrow(new IllegalArgumentException("No encontrado"));

        ResponseEntity<CompleteChallengResponseDto> result = controller.completeChallenge(99L, user);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void completeChallengeAlreadyCompletedReturns400Test() {
        when(challengeService.completeChallenge(1L, user))
                .thenThrow(new IllegalStateException("Ya completado"));

        ResponseEntity<CompleteChallengResponseDto> result = controller.completeChallenge(1L, user);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void completeChallengeLevelTooLowReturns400Test() {
        when(challengeService.completeChallenge(1L, user))
                .thenThrow(new IllegalStateException("No tienes acceso"));

        ResponseEntity<CompleteChallengResponseDto> result = controller.completeChallenge(1L, user);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void completeChallengeExpiredChallengeReturns400Test() {
        when(challengeService.completeChallenge(1L, user))
                .thenThrow(new IllegalStateException("Expirado"));

        ResponseEntity<CompleteChallengResponseDto> result = controller.completeChallenge(1L, user);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    // ─── getStats ─────────────────────────────────────────────────

    @Test
    void getStatsReturnsStatsTest() {
        StatsDto stats = new StatsDto(3, 7, 150, 2, List.of(LocalDate.now()), 2);
        when(challengeService.getStats(user)).thenReturn(stats);

        ResponseEntity<StatsDto> result = controller.getStats(user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(stats, result.getBody());
        assertEquals(3, result.getBody().streak());
        assertEquals(7, result.getBody().bestStreak());
    }

    @Test
    void getStatsZeroStatsReturnsOkTest() {
        StatsDto stats = new StatsDto(0, 0, 0, 1, List.of(), 0);
        when(challengeService.getStats(user)).thenReturn(stats);

        ResponseEntity<StatsDto> result = controller.getStats(user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(0, result.getBody().streak());
    }

    @Test
    void completeChallengeDataIntegrityViolationReturns400Test() {
        when(challengeService.completeChallenge(1L, user))
                .thenThrow(new DataIntegrityViolationException("Duplicate"));

        ResponseEntity<CompleteChallengResponseDto> result = controller.completeChallenge(1L, user);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }
}
