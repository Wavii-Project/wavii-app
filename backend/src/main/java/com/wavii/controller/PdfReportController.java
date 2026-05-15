package com.wavii.controller;

import com.wavii.model.PdfDocument;
import com.wavii.model.PdfReport;
import com.wavii.model.User;
import com.wavii.repository.PdfReportRepository;
import com.wavii.service.PdfStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pdfs")
@RequiredArgsConstructor
public class PdfReportController {

    private final PdfStorageService pdfStorageService;
    private final PdfReportRepository pdfReportRepository;

    @PostMapping("/{id}/report")
    public ResponseEntity<?> reportPdf(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            @RequestBody ReportRequest request) {
        PdfDocument pdf = pdfStorageService.findById(id);

        PdfReport report = PdfReport.builder()
                .pdfDocument(pdf)
                .reporter(currentUser)
                .reason(request.reason())
                .details(request.details())
                .build();
        pdfReportRepository.save(report);

        return ResponseEntity.ok(Map.of("message", "Reporte enviado"));
    }

    record ReportRequest(String reason, String details) {}
}
