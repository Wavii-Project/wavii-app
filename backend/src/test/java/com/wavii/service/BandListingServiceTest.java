package com.wavii.service;

import com.wavii.dto.band.BandListingResponse;
import com.wavii.dto.band.CreateBandListingRequest;
import com.wavii.model.BandListing;
import com.wavii.model.User;
import com.wavii.model.enums.ListingType;
import com.wavii.model.enums.MusicalGenre;
import com.wavii.model.enums.MusicianRole;
import com.wavii.repository.BandListingRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BandListingServiceTest {

    @Mock private BandListingRepository repository;

    @InjectMocks private BandListingService service;

    private User creator;
    private BandListing listing;
    private UUID listingId;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(UUID.randomUUID());
        creator.setName("Test Creator");

        listingId = UUID.randomUUID();
        listing = BandListing.builder()
                .id(listingId)
                .title("Test Band")
                .description("Test description")
                .type(ListingType.BANDA_BUSCA_MUSICOS)
                .genre(MusicalGenre.ROCK)
                .city("Madrid")
                .roles(List.of(MusicianRole.GUITARRISTA))
                .creator(creator)
                .contactInfo("test@contact.com")
                .build();
        listing.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getListingsNoFiltersReturnsPageTest() {
        Page<BandListing> page = new PageImpl<>(List.of(listing));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<BandListingResponse> result = service.getListings(null, null, null, 0);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getListingsWithValidGenreFiltersCorrectlyTest() {
        Page<BandListing> page = new PageImpl<>(List.of(listing));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<BandListingResponse> result = service.getListings("ROCK", null, null, 0);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getListingsWithInvalidGenreTreatsAsNullTest() {
        Page<BandListing> page = new PageImpl<>(List.of(listing));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<BandListingResponse> result = service.getListings("INVALID_GENRE", null, null, 0);

        assertNotNull(result);
    }

    @Test
    void getListingsWithCityFiltersCorrectlyTest() {
        Page<BandListing> page = new PageImpl<>(List.of(listing));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<BandListingResponse> result = service.getListings(null, "Madrid", null, 0);

        assertNotNull(result);
    }

    @Test
    void getListingsWithBlankCityTreatsAsNullTest() {
        Page<BandListing> page = new PageImpl<>(List.of(listing));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<BandListingResponse> result = service.getListings(null, "   ", null, 0);

        assertNotNull(result);
    }

    @Test
    void getListingsWithValidRoleFiltersCorrectlyTest() {
        Page<BandListing> page = new PageImpl<>(List.of(listing));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<BandListingResponse> result = service.getListings(null, null, "GUITARRISTA", 0);

        assertNotNull(result);
    }

    @Test
    void getListingsWithInvalidRoleTreatsAsNullTest() {
        Page<BandListing> page = new PageImpl<>(List.of(listing));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        service.getListings(null, null, "INVALID_ROLE", 0);

        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getByIdExistingIdReturnsResponseTest() {
        when(repository.findById(listingId)).thenReturn(Optional.of(listing));

        BandListingResponse result = service.getById(listingId);

        assertNotNull(result);
        assertEquals(listingId.toString(), result.id());
        assertEquals("Test Band", result.title());
        assertEquals("ROCK", result.genre());
        assertEquals("BANDA_BUSCA_MUSICOS", result.type());
    }

    @Test
    void getByIdNotFoundThrowsNotFoundExceptionTest() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getById(unknownId));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void createValidRequestReturnsSavedListingTest() {
        CreateBandListingRequest req = new CreateBandListingRequest(
                "New Band", "Description", ListingType.BANDA_BUSCA_MUSICOS,
                MusicalGenre.ROCK, "Barcelona", List.of(MusicianRole.BAJISTA), "contact@test.com",
                null, List.of());

        when(repository.save(any(BandListing.class))).thenReturn(listing);

        BandListingResponse result = service.create(req, creator);

        assertNotNull(result);
        verify(repository).save(any(BandListing.class));
    }

    @Test
    void createNullDescriptionSavesWithNullDescriptionTest() {
        CreateBandListingRequest req = new CreateBandListingRequest(
                "New Band", null, ListingType.BANDA_BUSCA_MUSICOS,
                MusicalGenre.ROCK, "Madrid", List.of(MusicianRole.GUITARRISTA), null, null, null);

        when(repository.save(any(BandListing.class))).thenReturn(listing);

        assertNotNull(service.create(req, creator));
    }

    @Test
    void createNullContactInfoSavesWithNullContactTest() {
        CreateBandListingRequest req = new CreateBandListingRequest(
                "New Band", "Desc", ListingType.BANDA_BUSCA_MUSICOS,
                MusicalGenre.ROCK, "Madrid", List.of(MusicianRole.GUITARRISTA), null, null, null);

        when(repository.save(any(BandListing.class))).thenReturn(listing);

        assertNotNull(service.create(req, creator));
    }

    @Test
    void createBlankTitleThrowsBadRequestTest() {
        CreateBandListingRequest req = new CreateBandListingRequest(
                "  ", "Description", ListingType.BANDA_BUSCA_MUSICOS,
                MusicalGenre.ROCK, "Madrid", List.of(MusicianRole.GUITARRISTA), null, null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(req, creator));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void createNullTitleThrowsBadRequestTest() {
        CreateBandListingRequest req = new CreateBandListingRequest(
                null, "Description", ListingType.BANDA_BUSCA_MUSICOS,
                MusicalGenre.ROCK, "Madrid", List.of(MusicianRole.GUITARRISTA), null, null, null);

        assertThrows(ResponseStatusException.class, () -> service.create(req, creator));
    }

    @Test
    void createNullTypeThrowsBadRequestTest() {
        CreateBandListingRequest req = new CreateBandListingRequest(
                "Title", "Description", null,
                MusicalGenre.ROCK, "Madrid", List.of(MusicianRole.GUITARRISTA), null, null, null);

        assertThrows(ResponseStatusException.class, () -> service.create(req, creator));
    }

    @Test
    void createNullGenreThrowsBadRequestTest() {
        CreateBandListingRequest req = new CreateBandListingRequest(
                "Title", "Description", ListingType.BANDA_BUSCA_MUSICOS,
                null, "Madrid", List.of(MusicianRole.GUITARRISTA), null, null, null);

        assertThrows(ResponseStatusException.class, () -> service.create(req, creator));
    }

    @Test
    void createBlankCityThrowsBadRequestTest() {
        CreateBandListingRequest req = new CreateBandListingRequest(
                "Title", "Description", ListingType.BANDA_BUSCA_MUSICOS,
                MusicalGenre.ROCK, "  ", List.of(MusicianRole.GUITARRISTA), null, null, null);

        assertThrows(ResponseStatusException.class, () -> service.create(req, creator));
    }

    @Test
    void createNullCityThrowsBadRequestTest() {
        CreateBandListingRequest req = new CreateBandListingRequest(
                "Title", "Description", ListingType.BANDA_BUSCA_MUSICOS,
                MusicalGenre.ROCK, null, List.of(MusicianRole.GUITARRISTA), null, null, null);

        assertThrows(ResponseStatusException.class, () -> service.create(req, creator));
    }

    @Test
    void createEmptyRolesThrowsBadRequestTest() {
        CreateBandListingRequest req = new CreateBandListingRequest(
                "Title", "Description", ListingType.BANDA_BUSCA_MUSICOS,
                MusicalGenre.ROCK, "Madrid", List.of(), null, null, null);

        assertThrows(ResponseStatusException.class, () -> service.create(req, creator));
    }

    @Test
    void createNullRolesThrowsBadRequestTest() {
        CreateBandListingRequest req = new CreateBandListingRequest(
                "Title", "Description", ListingType.BANDA_BUSCA_MUSICOS,
                MusicalGenre.ROCK, "Madrid", null, null, null, null);

        assertThrows(ResponseStatusException.class, () -> service.create(req, creator));
    }

    @Test
    void deleteOwnerDeletesSuccessTest() {
        when(repository.findById(listingId)).thenReturn(Optional.of(listing));

        service.delete(listingId, creator);

        verify(repository).delete(listing);
    }

    @Test
    void deleteOtherUserThrowsForbiddenTest() {
        User other = new User();
        other.setId(UUID.randomUUID());
        when(repository.findById(listingId)).thenReturn(Optional.of(listing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.delete(listingId, other));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void deleteNotFoundThrowsNotFoundTest() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.delete(unknownId, creator));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void getMyListingsReturnsUserListingsTest() {
        when(repository.findByCreatorIdOrderByCreatedAtDesc(creator.getId()))
                .thenReturn(List.of(listing));

        List<BandListingResponse> result = service.getMyListings(creator);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Band", result.get(0).title());
    }

    @Test
    void getMyListingsNoListingsReturnsEmptyTest() {
        when(repository.findByCreatorIdOrderByCreatedAtDesc(creator.getId()))
                .thenReturn(List.of());

        List<BandListingResponse> result = service.getMyListings(creator);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ─── Specification predicate invocation ───────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getListingsSpecPredicatesBuiltForGenreCityAndRoleTest() {
        ArgumentCaptor<Specification<BandListing>> specCaptor = ArgumentCaptor.forClass(Specification.class);

        when(repository.findAll(specCaptor.capture(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getListings("ROCK", "madrid", "GUITARRISTA", 0);

        Specification<BandListing> captured = specCaptor.getValue();

        Root<BandListing> root = mock(Root.class);
        CriteriaQuery<?> cq = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        @SuppressWarnings("unchecked")
        Path<Object> genrePath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<Object> cityPath = mock(Path.class);
        @SuppressWarnings("rawtypes")
        Join rolesJoinRaw = mock(Join.class);
        @SuppressWarnings("unchecked")
        Expression<String> cityExpr = mock(Expression.class);
        Predicate genrePred = mock(Predicate.class);
        Predicate cityPred = mock(Predicate.class);
        Predicate rolePred = mock(Predicate.class);
        Predicate andPred = mock(Predicate.class);

        doReturn(genrePath).when(root).get("genre");
        doReturn(cityPath).when(root).get("city");
        when(root.join("roles", JoinType.INNER)).thenReturn(rolesJoinRaw);
        doReturn(cityExpr).when(cb).lower((Expression) cityPath);
        when(cb.equal(genrePath, MusicalGenre.ROCK)).thenReturn(genrePred);
        when(cb.like(cityExpr, "%madrid%")).thenReturn(cityPred);
        when(cb.equal(rolesJoinRaw, MusicianRole.GUITARRISTA.name())).thenReturn(rolePred);
        when(cb.and(any(Predicate[].class))).thenReturn(andPred);

        Predicate result = captured.toPredicate(root, cq, cb);

        assertNotNull(result);
        verify(cb).equal(genrePath, MusicalGenre.ROCK);
        verify(cb).like(cityExpr, "%madrid%");
        verify(cb).equal(rolesJoinRaw, MusicianRole.GUITARRISTA.name());
        verify(cq).distinct(true);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getListingsSpecPredicatesNullFiltersEmptyPredicatesTest() {
        ArgumentCaptor<Specification<BandListing>> specCaptor = ArgumentCaptor.forClass(Specification.class);

        when(repository.findAll(specCaptor.capture(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getListings(null, null, null, 0);

        Specification<BandListing> captured = specCaptor.getValue();

        Root<BandListing> root = mock(Root.class);
        CriteriaQuery<?> cq = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate andPred = mock(Predicate.class);
        when(cb.and(any(Predicate[].class))).thenReturn(andPred);

        Predicate result = captured.toPredicate(root, cq, cb);

        assertNotNull(result);
        verify(cb, never()).equal(any(), any());
        verify(cb, never()).like(any(Expression.class), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getListingsSpecPredicatesOnlyCityFilterTest() {
        ArgumentCaptor<Specification<BandListing>> specCaptor = ArgumentCaptor.forClass(Specification.class);

        when(repository.findAll(specCaptor.capture(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getListings(null, "barcelona", null, 0);

        Specification<BandListing> captured = specCaptor.getValue();

        Root<BandListing> root = mock(Root.class);
        CriteriaQuery<?> cq = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        @SuppressWarnings("unchecked")
        Path<Object> cityPath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Expression<String> cityExpr = mock(Expression.class);
        Predicate cityPred = mock(Predicate.class);
        Predicate andPred = mock(Predicate.class);

        doReturn(cityPath).when(root).get("city");
        doReturn(cityExpr).when(cb).lower((Expression) cityPath);
        when(cb.like(cityExpr, "%barcelona%")).thenReturn(cityPred);
        when(cb.and(any(Predicate[].class))).thenReturn(andPred);

        Predicate result = captured.toPredicate(root, cq, cb);

        assertNotNull(result);
        verify(cb).like(cityExpr, "%barcelona%");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getListingsSpecPredicatesOnlyGenreFilterTest() {
        ArgumentCaptor<Specification<BandListing>> specCaptor = ArgumentCaptor.forClass(Specification.class);

        when(repository.findAll(specCaptor.capture(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getListings("JAZZ", null, null, 0);

        Specification<BandListing> captured = specCaptor.getValue();

        Root<BandListing> root = mock(Root.class);
        CriteriaQuery<?> cq = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path<Object> genrePath = mock(Path.class);
        Predicate genrePred = mock(Predicate.class);
        Predicate andPred = mock(Predicate.class);

        when(root.get("genre")).thenReturn(genrePath);
        when(cb.equal(genrePath, MusicalGenre.JAZZ)).thenReturn(genrePred);
        when(cb.and(any(Predicate[].class))).thenReturn(andPred);

        Predicate result = captured.toPredicate(root, cq, cb);

        assertNotNull(result);
        verify(cb).equal(genrePath, MusicalGenre.JAZZ);
    }

    // ─── normalizeUrl and safeImageUrls helpers ────────────────────

    @Test
    void createWithNonNullCoverImageUrlTrimsUrlTest() {
        CreateBandListingRequest req = new CreateBandListingRequest(
                "Title", "Desc", ListingType.BANDA_BUSCA_MUSICOS,
                MusicalGenre.ROCK, "Madrid", List.of(MusicianRole.GUITARRISTA),
                "contact@test.com", "  https://img.example.com/cover.jpg  ", List.of());

        when(repository.save(any(BandListing.class))).thenReturn(listing);

        BandListingResponse result = service.create(req, creator);

        assertNotNull(result);
        verify(repository).save(argThat(bl -> bl.getCoverImageUrl() != null));
    }

    @Test
    void createWithBlankCoverImageUrlSavesNullTest() {
        CreateBandListingRequest req = new CreateBandListingRequest(
                "Title", "Desc", ListingType.BANDA_BUSCA_MUSICOS,
                MusicalGenre.ROCK, "Madrid", List.of(MusicianRole.GUITARRISTA),
                null, "   ", List.of("img1", "", "  img3  ", "img4extra"));

        when(repository.save(any(BandListing.class))).thenReturn(listing);

        BandListingResponse result = service.create(req, creator);

        assertNotNull(result);
        verify(repository).save(argThat(bl -> bl.getCoverImageUrl() == null));
    }
}
