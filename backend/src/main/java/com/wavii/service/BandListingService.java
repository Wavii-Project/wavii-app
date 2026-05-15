package com.wavii.service;

import com.wavii.dto.band.BandListingResponse;
import com.wavii.dto.band.CreateBandListingRequest;
import com.wavii.model.BandListing;
import com.wavii.model.User;
import com.wavii.model.enums.MusicalGenre;
import com.wavii.model.enums.MusicianRole;
import com.wavii.repository.BandListingRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BandListingService {

    private final BandListingRepository repository;

    @Transactional(readOnly = true)
    public Page<BandListingResponse> getListings(String genreStr, String city, String roleStr, int page) {
        MusicalGenre genre = parseEnum(MusicalGenre.class, genreStr);
        MusicianRole role  = parseEnum(MusicianRole.class, roleStr);
        String cityFilter  = (city != null && !city.isBlank()) ? city.trim().toLowerCase() : null;

        Specification<BandListing> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (genre != null) {
                predicates.add(cb.equal(root.get("genre"), genre));
            }
            if (cityFilter != null) {
                predicates.add(cb.like(cb.lower(root.get("city")), "%" + cityFilter + "%"));
            }
            if (role != null) {
                Join<BandListing, String> rolesJoin = root.join("roles", JoinType.INNER);
                predicates.add(cb.equal(rolesJoin, role.name()));
                query.distinct(true);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return repository.findAll(spec, PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BandListingResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public BandListingResponse create(CreateBandListingRequest req, User creator) {
        validate(req);
        BandListing listing = BandListing.builder()
                .title(req.title().trim())
                .description(req.description() != null ? req.description().trim() : null)
                .type(req.type())
                .genre(req.genre())
                .city(req.city().trim())
                .roles(req.roles() != null ? req.roles() : List.of())
                .creator(creator)
                .contactInfo(req.contactInfo() != null ? req.contactInfo().trim() : null)
                .coverImageUrl(normalizeUrl(req.coverImageUrl()))
                .imageUrls(safeImageUrls(req.imageUrls()))
                .build();
        return toResponse(repository.save(listing));
    }

    @Transactional
    public void delete(UUID id, User requester) {
        BandListing listing = findOrThrow(id);
        if (!listing.getCreator().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes eliminar este anuncio");
        }
        repository.delete(listing);
    }

    @Transactional(readOnly = true)
    public List<BandListingResponse> getMyListings(User user) {
        return repository.findByCreatorIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private BandListing findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anuncio no encontrado"));
    }

    private void validate(CreateBandListingRequest req) {
        if (req.title() == null || req.title().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título es obligatorio");
        if (req.type() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo es obligatorio");
        if (req.genre() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El género es obligatorio");
        if (req.city() == null || req.city().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La ciudad es obligatoria");
        if (req.roles() == null || req.roles().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona al menos un rol");
    }

    private BandListingResponse toResponse(BandListing bl) {
        return new BandListingResponse(
                bl.getId().toString(),
                bl.getTitle(),
                bl.getDescription(),
                bl.getType().name(),
                bl.getGenre().name(),
                bl.getCity(),
                bl.getRoles().stream().map(Enum::name).toList(),
                bl.getCreator().getId().toString(),
                bl.getCreator().getName(),
                bl.getContactInfo(),
                bl.getCoverImageUrl(),
                safeImageUrls(bl.getImageUrls()),
                bl.getCreatedAt().toString()
        );
    }

    private String normalizeUrl(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private List<String> safeImageUrls(List<String> urls) {
        if (urls == null) return List.of();
        return urls.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .limit(3)
                .toList();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> cls, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(cls, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
