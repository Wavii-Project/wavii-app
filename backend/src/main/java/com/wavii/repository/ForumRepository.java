package com.wavii.repository;

import com.wavii.model.Forum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositorio para la gestión de persistencia de foros y comunidades.
 * 
 * @author danielrguezh
 */
@Repository
public interface ForumRepository extends JpaRepository<Forum, UUID>, JpaSpecificationExecutor<Forum> {

    /** Busca foros aplicando criterios de filtrado y carga el creador de forma eficiente. */
    @EntityGraph(attributePaths = "creator")
    @Override
    org.springframework.data.domain.Page<Forum> findAll(Specification<Forum> spec, org.springframework.data.domain.Pageable pageable);

    /** Incrementa el contador de miembros de un foro. */
    @Modifying
    @Query("UPDATE Forum f SET f.memberCount = f.memberCount + 1 WHERE f.id = :id")
    void incrementMemberCount(@Param("id") UUID id);

    /** Decrementa el contador de miembros de un foro. */
    @Modifying
    @Query("UPDATE Forum f SET f.memberCount = f.memberCount - 1 WHERE f.id = :id AND f.memberCount > 0")
    void decrementMemberCount(@Param("id") UUID id);

    /** Incrementa el contador de "me gusta" de un foro. */
    @Modifying
    @Query("UPDATE Forum f SET f.likeCount = f.likeCount + 1 WHERE f.id = :id")
    void incrementLikeCount(@Param("id") UUID id);

    /** Decrementa el contador de "me gusta" de un foro. */
    @Modifying
    @Query("UPDATE Forum f SET f.likeCount = f.likeCount - 1 WHERE f.id = :id AND f.likeCount > 0")
    void decrementLikeCount(@Param("id") UUID id);
}
