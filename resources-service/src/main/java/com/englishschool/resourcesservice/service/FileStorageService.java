package com.englishschool.resourcesservice.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.nio.file.*;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.storage.s3.enabled:false}")
    private boolean s3Enabled;

    @Value("${app.storage.s3.endpoint:}")
    private String endpoint;

    @Value("${app.storage.s3.region:us-east-1}")
    private String region;

    @Value("${app.storage.s3.bucket:}")
    private String bucket;

    @Value("${app.storage.s3.access-key:}")
    private String accessKey;

    @Value("${app.storage.s3.secret-key:}")
    private String secretKey;

    @Value("${app.storage.s3.public-url-prefix:}")
    private String publicUrlPrefix;

    @Value("${app.storage.local-dir:uploads}")
    private String localDir;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        if (!s3Enabled || endpoint.isBlank() || accessKey.isBlank()) return;
        try {
            s3Client = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .forcePathStyle(true)
                    .build();
            log.info("S3 storage ready — bucket: {}, endpoint: {}", bucket, endpoint);
        } catch (Exception e) {
            log.warn("S3 init failed, will use local storage: {}", e.getMessage());
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new RuntimeException("Empty file");

        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.')).toLowerCase()
                : "";
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID() + ext;

        if (s3Client != null) {
            try {
                return uploadToS3(file, fileName);
            } catch (Exception e) {
                log.warn("S3 upload failed, falling back to local storage: {}", e.getMessage());
            }
        }

        return storeLocally(file, fileName);
    }

    private String uploadToS3(MultipartFile file, String fileName) throws Exception {
        String key = "resources/" + fileName;
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .contentLength(file.getSize())
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        String base = publicUrlPrefix.endsWith("/")
                ? publicUrlPrefix.substring(0, publicUrlPrefix.length() - 1)
                : publicUrlPrefix;
        return base + "/" + key;
    }

    private String storeLocally(MultipartFile file, String fileName) {
        try {
            Path dir = Paths.get(localDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            return "/" + localDir + "/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("File storage failed: " + e.getMessage(), e);
        }
    }
}
