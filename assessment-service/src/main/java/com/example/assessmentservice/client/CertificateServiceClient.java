package com.example.assessmentservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

/**
 * Client HTTP vers le Certificate Service (FastAPI Python - port 8097).
 * Appelé depuis GradeService quand un étudiant réussit un examen.
 */
@Service
@Slf4j
public class CertificateServiceClient {

    private final WebClient webClient;

    public CertificateServiceClient(
            @Value("${certificate.service.url:http://localhost:8097}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record CertificateRequest(
            String studentName,
            String studentEmail,
            String examTitle,
            double score,
            double maxScore,
            String passedAt
    ) {}

    public record CertificateResponse(
            String certificateId,
            String studentName,
            String examTitle,
            double score,
            String downloadUrl,
            String generatedAt
    ) {}

    // ── Méthodes ──────────────────────────────────────────────────────────────

    /**
     * Génération asynchrone — ne bloque pas la réponse HTTP principale.
     * Les erreurs sont loggées sans faire planter le flux principal.
     */
    public void generateCertificateAsync(CertificateRequest request) {
        webClient.post()
                .uri("/api/certificates/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CertificateResponse.class)
                .doOnSuccess(resp -> log.info(
                        "✅ Certificat généré pour {} | ID: {} | URL: {}",
                        resp.studentName(), resp.certificateId(), resp.downloadUrl()))
                .doOnError(err -> log.error(
                        "❌ Échec génération certificat pour {}: {}",
                        request.studentName(), err.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }

    /**
     * Génération synchrone — retourne l'URL de téléchargement directement.
     */
    public CertificateResponse generateCertificateSync(CertificateRequest request) {
        return webClient.post()
                .uri("/api/certificates/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CertificateResponse.class)
                .block();
    }
}