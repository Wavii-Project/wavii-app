package com.wavii.repository;

import com.wavii.model.BandListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface BandListingRepository extends JpaRepository<BandListing, UUID>, JpaSpecificationExecutor<BandListing> {

    @EntityGraph(attributePaths = "creator")
    @Override
    Page<BandListing> findAll(Specification<BandListing> spec, Pageable pageable);

    List<BandListing> findByCreatorIdOrderByCreatedAtDesc(UUID creatorId);
}
