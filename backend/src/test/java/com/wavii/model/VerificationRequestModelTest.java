package com.wavii.model;

import com.wavii.model.enums.VerificationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VerificationRequestModelTest {

    @Test
    void builderSetsAllFieldsTest() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setEmail("teacher@test.com");
        LocalDateTime now = LocalDateTime.now();

        VerificationRequest request = VerificationRequest.builder()
                .id(id)
                .user(user)
                .fileName("certificate.pdf")
                .filePath("/app/uploads/verifications/cert.pdf")
                .status(VerificationStatus.PENDING)
                .createdAt(now)
                .build();

        assertEquals(id, request.getId());
        assertEquals(user, request.getUser());
        assertEquals("certificate.pdf", request.getFileName());
        assertEquals("/app/uploads/verifications/cert.pdf", request.getFilePath());
        assertEquals(VerificationStatus.PENDING, request.getStatus());
        assertEquals(now, request.getCreatedAt());
    }

    @Test
    void builderDefaultStatusIsPendingTest() {
        VerificationRequest request = VerificationRequest.builder()
                .fileName("doc.pdf")
                .filePath("/path/to/doc.pdf")
                .build();

        assertEquals(VerificationStatus.PENDING, request.getStatus());
    }

    @Test
    void builderDefaultCreatedAtIsSetTest() {
        VerificationRequest request = VerificationRequest.builder()
                .fileName("doc.pdf")
                .filePath("/path/to/doc.pdf")
                .build();

        assertNotNull(request.getCreatedAt());
    }

    @Test
    void settersUpdateStatusTest() {
        VerificationRequest request = VerificationRequest.builder()
                .fileName("doc.pdf")
                .filePath("/path/to/doc.pdf")
                .status(VerificationStatus.PENDING)
                .build();

        request.setStatus(VerificationStatus.APPROVED);
        assertEquals(VerificationStatus.APPROVED, request.getStatus());

        request.setStatus(VerificationStatus.REJECTED);
        assertEquals(VerificationStatus.REJECTED, request.getStatus());
    }

    @Test
    void noArgsConstructorCreatesEmptyRequestTest() {
        VerificationRequest request = new VerificationRequest();
        assertNull(request.getId());
        assertNull(request.getFileName());
    }

    @Test
    void allArgsConstructorSetsAllFieldsTest() {
        UUID id = UUID.randomUUID();
        User user = new User();
        LocalDateTime now = LocalDateTime.now();

        VerificationRequest request = new VerificationRequest(
                id, user, "file.pdf", "/path/file.pdf", VerificationStatus.APPROVED, now);

        assertEquals(id, request.getId());
        assertEquals(user, request.getUser());
        assertEquals("file.pdf", request.getFileName());
        assertEquals("/path/file.pdf", request.getFilePath());
        assertEquals(VerificationStatus.APPROVED, request.getStatus());
        assertEquals(now, request.getCreatedAt());
    }
}
