package com.wavii.repository;

import com.wavii.model.BandListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio para la gestión de persistencia de anuncios de bandas y músicos.
 * 
 * @author danielrguezh
 */
@Repository
public interface BandListingRepository extends JpaRepository<BandListing, UUID>, JpaSpecificationExecutor<BandListing> {

    /** Busca anuncios aplicando criterios de filtrado y carga el creador de forma eficiente. */
    @EntityGraph(attributePaths = "creator")
    @Override
    Page<BandListing> findAll(Specification<BandListing> spec, Pageable pageable);

    /** Lista todos los anuncios creados por un usuario específico. */
    List<BandListing> findByCreatorIdOrderByCreatedAtDesc(UUID creatorId);
}
