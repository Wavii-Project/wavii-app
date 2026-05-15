package com.wavii.dto.challenge;

import com.wavii.model.DailyChallenge;
import com.wavii.model.enums.Level;

import java.time.LocalDate;

/**
 * DTO que representa un desafío diario para ser enviado al cliente.
 * Incluye información sobre la tablatura asociada y el estado de completado para el usuario actual.
 */
public record DailyChallengeDto(
        Long id,
        LocalDate challengeDate,
        Level difficulty,
        int xpReward,
        Long tabId,
        String tabTitle,
        String tabOwnerName,
        String tabDescription,
        String tabCoverImageUrl,
        boolean completedByMe
) {
    public static DailyChallengeDto from(DailyChallenge challenge, boolean completedByMe) {
        return new DailyChallengeDto(
                challenge.getId(),
                challenge.getChallengeDate(),
                challenge.getDifficulty(),
                challenge.getXpReward(),
                challenge.getPdfDocument().getId(),
                challenge.getPdfDocument().getSongTitle(),
                challenge.getPdfDocument().getOwner() != null
                        ? challenge.getPdfDocument().getOwner().getName()
                        : null,
                challenge.getPdfDocument().getDescription(),
                challenge.getPdfDocument().getCoverImagePath() != null
                        ? "/uploads/" + challenge.getPdfDocument().getCoverImagePath()
                        : null,
                completedByMe
        );
    }
}
