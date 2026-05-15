package com.wavii.repository;

import com.wavii.model.DailyChallenge;
import com.wavii.model.User;
import com.wavii.model.UserChallengeCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface UserChallengeCompletionRepository extends JpaRepository<UserChallengeCompletion, Long> {

    /** Comprueba si el usuario ya completo un desafio concreto */
    boolean existsByUserAndDailyChallenge(User user, DailyChallenge challenge);

    /** Dias en los que el usuario completo al menos un desafio en un rango de fechas */
    @Query("""
            SELECT DISTINCT ucc.completedDate
            FROM UserChallengeCompletion ucc
            WHERE ucc.user.id = :userId
              AND ucc.completedDate BETWEEN :from AND :to
            ORDER BY ucc.completedDate
            """)
    List<LocalDate> findCompletedDatesByUserInRange(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    /** Todos los desafios completados hoy por el usuario */
    @Query("""
            SELECT ucc FROM UserChallengeCompletion ucc
            WHERE ucc.user.id = :userId
              AND ucc.completedDate = :date
            """)
    List<UserChallengeCompletion> findByUserIdAndDate(
            @Param("userId") UUID userId,
            @Param("date") LocalDate date
    );
}
