package com.wavii.controller;

import com.wavii.model.PdfDocument;
import com.wavii.model.PdfReport;
import com.wavii.model.User;
import com.wavii.repository.PdfReportRepository;
import com.wavii.service.PdfStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfReportControllerTest {

    @Mock
    private PdfStorageService pdfStorageService;

    @Mock
    private PdfReportRepository pdfReportRepository;

    @InjectMocks
    private PdfReportController pdfReportController;

    private User reporter;
    private PdfDocument pdfDocument;

    @BeforeEach
    void setUp() {
        reporter = new User();
        reporter.setId(UUID.randomUUID());
        reporter.setName("Reporter");
        reporter.setEmail("reporter@wavii.app");

        pdfDocument = PdfDocument.builder()
                .id(1L)
                .originalName("test.pdf")
                .fileName("test_stored.pdf")
                .filePath("/uploads/test_stored.pdf")
                .build();
    }

    @Test
    void reportPdfSuccessReturnsOkTest() {
        when(pdfStorageService.findById(1L)).thenReturn(pdfDocument);
        when(pdfReportRepository.save(any(PdfReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PdfReportController.ReportRequest request = new PdfReportController.ReportRequest("spam", "Contenido inapropiado");
        ResponseEntity<?> result = pdfReportController.reportPdf(1L, reporter, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertEquals("Reporte enviado", body.get("message"));
        verify(pdfStorageService).findById(1L);
        verify(pdfReportRepository).save(any(PdfReport.class));
    }

    @Test
    void reportPdfSavesCorrectReasonAndDetailsTest() {
        when(pdfStorageService.findById(1L)).thenReturn(pdfDocument);
        when(pdfReportRepository.save(any(PdfReport.class))).thenAnswer(invocation -> {
            PdfReport saved = invocation.getArgument(0);
            assertEquals("contenido_ilegal", saved.getReason());
            assertEquals("Hay material protegido por derechos de autor", saved.getDetails());
            assertEquals(reporter, saved.getReporter());
            assertEquals(pdfDocument, saved.getPdfDocument());
            return saved;
        });

        PdfReportController.ReportRequest request = new PdfReportController.ReportRequest(
                "contenido_ilegal", "Hay material protegido por derechos de autor");
        pdfReportController.reportPdf(1L, reporter, request);

        verify(pdfReportRepository).save(any(PdfReport.class));
    }

    @Test
    void reportPdfPdfNotFoundPropagatesExceptionTest() {
        when(pdfStorageService.findById(99L)).thenThrow(new IllegalArgumentException("PDF no encontrado"));

        PdfReportController.ReportRequest request = new PdfReportController.ReportRequest("spam", "Detalles");

        assertThrows(IllegalArgumentException.class, () ->
                pdfReportController.reportPdf(99L, reporter, request));
    }

    @Test
    void reportPdfWithNullDetailsStillSucceedsTest() {
        when(pdfStorageService.findById(1L)).thenReturn(pdfDocument);
        when(pdfReportRepository.save(any(PdfReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PdfReportController.ReportRequest request = new PdfReportController.ReportRequest("spam", null);
        ResponseEntity<?> result = pdfReportController.reportPdf(1L, reporter, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}
