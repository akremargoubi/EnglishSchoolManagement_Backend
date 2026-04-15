package com.englishschool.enrollmentservice.controller;

import com.englishschool.enrollmentservice.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
@Tag(name = "Certificates", description = "Certificate generation (PDF)")
public class CertificateController {

    private final CertificateService certificateService;

    @GetMapping("/enrollment/{enrollmentId}/download")
    @Operation(summary = "Download certificate as PDF")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable Long enrollmentId) {
        byte[] pdf = certificateService.generateCertificatePdf(enrollmentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificate-" + enrollmentId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
