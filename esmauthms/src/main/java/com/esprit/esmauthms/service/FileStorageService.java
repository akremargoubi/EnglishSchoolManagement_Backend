package com.esprit.esmauthms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.storage.avatar-dir:avatars}")
    private String avatarDir;

    @Value("${app.storage.s3.enabled:false}")
    private boolean s3Enabled;

    @Value("${app.storage.s3.bucket:}")
    private String s3Bucket;

    @Value("${app.storage.s3.public-url-prefix:}")
    private String s3PublicUrlPrefix;

    // Optional — only present when s3.enabled=true
    @Autowired(required = false)
    private S3Client s3Client;

    public String storeAvatar(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Empty file");
        }

        String extension = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            extension = original.substring(original.lastIndexOf(".")).toLowerCase();
        }
        String fileName = UUID.randomUUID() + extension;

        if (s3Enabled && s3Client != null) {
            try {
                return uploadToS3(file, fileName);
            } catch (Exception e) {
                log.warn("S3 upload failed, falling back to local storage: {}", e.getMessage());
            }
        }

        return storeLocally(file, fileName);
    }

    private String uploadToS3(MultipartFile file, String fileName) throws IOException {
        String key = "avatars/" + fileName;
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(putReq, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        String base = s3PublicUrlPrefix.endsWith("/")
                ? s3PublicUrlPrefix.substring(0, s3PublicUrlPrefix.length() - 1)
                : s3PublicUrlPrefix;
        return base + "/" + key;
    }

    private String storeLocally(MultipartFile file, String fileName) {
        try {
            Path uploadDir = Paths.get(avatarDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/files/avatars/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Could not store file locally", e);
        }
    }
}
