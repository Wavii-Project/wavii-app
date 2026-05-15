package com.wavii.controller;

import com.wavii.dto.challenge.CompleteChallengResponseDto;
import com.wavii.dto.challenge.DailyChallengeDto;
import com.wavii.dto.challenge.StatsDto;
import com.wavii.model.User;
import com.wavii.service.DailyChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class DailyChallengeController {

    private final DailyChallengeService challengeService;

    /**
     * GET /api/challenges/today
     * Devuelve los desafios del dia visibles para el usuario segun su nivel.
     * Si aun no se han generado los desafios del dia, los genera en este momento.
     */
    @GetMapping("/today")
    public ResponseEntity<List<DailyChallengeDto>> getTodayChallenges(
            @AuthenticationPrincipal User currentUser) {
        List<DailyChallengeDto> challenges = challengeService.getTodayChallengesForUser(currentUser);
        return ResponseEntity.ok(challenges);
    }

    /**
     * POST /api/challenges/{id}/complete
     * Marca un desafio como completado por el usuario autenticado.
     * Actualiza XP, racha y nivel del usuario y devuelve el nuevo estado.
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<CompleteChallengResponseDto> completeChallenge(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        try {
            CompleteChallengResponseDto response = challengeService.completeChallenge(id, currentUser);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/challenges/stats
     * Devuelve las estadisticas del usuario: racha, mejor racha, XP, nivel
     * y dias completados del mes actual para el calendario.
     */
    @GetMapping("/stats")
    public ResponseEntity<StatsDto> getStats(
            @AuthenticationPrincipal User currentUser) {
        StatsDto stats = challengeService.getStats(currentUser);
        return ResponseEntity.ok(stats);
    }
}
