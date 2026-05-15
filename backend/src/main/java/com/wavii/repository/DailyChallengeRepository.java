package com.wavii.repository;

import com.wavii.model.DailyChallenge;
import com.wavii.model.enums.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface DailyChallengeRepository extends JpaRepository<DailyChallenge, Long> {

    /** Devuelve todos los desafios generados para una fecha concreta */
    List<DailyChallenge> findByChallengeDate(LocalDate date);

    /** Devuelve los desafios de una dificultad concreta para una fecha, ordenados por slot */
    List<DailyChallenge> findByChallengeDateAndDifficultyOrderBySlotAsc(LocalDate date, Level difficulty);

    List<DailyChallenge> findByChallengeDateGreaterThanEqual(LocalDate since);
}
