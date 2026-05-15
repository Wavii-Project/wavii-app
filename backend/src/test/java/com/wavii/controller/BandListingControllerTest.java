package com.wavii.controller;

import com.wavii.dto.band.BandListingResponse;
import com.wavii.dto.band.CreateBandListingRequest;
import com.wavii.model.User;
import com.wavii.model.enums.ListingType;
import com.wavii.model.enums.MusicalGenre;
import com.wavii.model.enums.MusicianRole;
import com.wavii.service.BandListingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BandListingControllerTest {

    @Mock private BandListingService service;

    @InjectMocks private BandListingController controller;

    private User user;
    private BandListingResponse sampleResponse;
    private UUID listingId;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());

        listingId = UUID.randomUUID();
        sampleResponse = new BandListingResponse(
                listingId.toString(), "Test Band", "Desc",
                "BAND_SEEKS_MUSICIAN", "ROCK", "Madrid",
                List.of("GUITARIST"), user.getId().toString(), "Test User",
                "contact@test.com", null, List.of(), "2024-01-01T00:00:00");
    }

    @Test
    void getListingsNoFiltersReturnsOkTest() {
        Page<BandListingResponse> page = new PageImpl<>(List.of(sampleResponse));
        when(service.getListings(null, null, null, 0)).thenReturn(page);

        ResponseEntity<Page<BandListingResponse>> result = controller.getListings(null, null, null, 0);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getTotalElements());
    }

    @Test
    void getListingsWithFiltersReturnsOkTest() {
        Page<BandListingResponse> page = new PageImpl<>(List.of(sampleResponse));
        when(service.getListings("ROCK", "Madrid", "GUITARIST", 0)).thenReturn(page);

        ResponseEntity<Page<BandListingResponse>> result =
                controller.getListings("ROCK", "Madrid", "GUITARIST", 0);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getListingsEmptyPageReturnsOkTest() {
        Page<BandListingResponse> page = new PageImpl<>(List.of());
        when(service.getListings(null, null, null, 0)).thenReturn(page);

        ResponseEntity<Page<BandListingResponse>> result =
                controller.getListings(null, null, null, 0);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(0, result.getBody().getTotalElements());
    }

    @Test
    void getMyListingsReturnsOkTest() {
        when(service.getMyListings(user)).thenReturn(List.of(sampleResponse));

        ResponseEntity<List<BandListingResponse>> result = controller.getMyListings(user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void getMyListingsEmptyReturnsOkWithEmptyListTest() {
        when(service.getMyListings(user)).thenReturn(List.of());

        ResponseEntity<List<BandListingResponse>> result = controller.getMyListings(user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    @Test
    void getListingExistingIdReturnsOkTest() {
        when(service.getById(listingId)).thenReturn(sampleResponse);

        ResponseEntity<BandListingResponse> result = controller.getListing(listingId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(sampleResponse, result.getBody());
    }

    @Test
    void getListingNotFoundPropagatesExceptionTest() {
        when(service.getById(listingId))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThrows(ResponseStatusException.class, () -> controller.getListing(listingId));
    }

    @Test
    void createValidRequestReturns201Test() {
        CreateBandListingRequest req = new CreateBandListingRequest(
                "New Band", "Desc", ListingType.BANDA_BUSCA_MUSICOS,
                MusicalGenre.ROCK, "Madrid", List.of(MusicianRole.GUITARRISTA), null, null, null);

        when(service.create(req, user)).thenReturn(sampleResponse);

        ResponseEntity<BandListingResponse> result = controller.create(req, user);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(sampleResponse, result.getBody());
    }

    @Test
    void createInvalidRequestPropagatesExceptionTest() {
        CreateBandListingRequest req = new CreateBandListingRequest(
                null, null, null, null, null, null, null, null, null);

        when(service.create(req, user))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST));

        assertThrows(ResponseStatusException.class, () -> controller.create(req, user));
    }

    @Test
    void deleteOwnerReturns204Test() {
        doNothing().when(service).delete(listingId, user);

        ResponseEntity<Void> result = controller.delete(listingId, user);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(service).delete(listingId, user);
    }

    @Test
    void deleteNotOwnerPropagatesExceptionTest() {
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
                .when(service).delete(listingId, user);

        assertThrows(ResponseStatusException.class, () -> controller.delete(listingId, user));
    }

    @Test
    void deleteNotFoundPropagatesExceptionTest() {
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND))
                .when(service).delete(listingId, user);

        assertThrows(ResponseStatusException.class, () -> controller.delete(listingId, user));
    }
}
