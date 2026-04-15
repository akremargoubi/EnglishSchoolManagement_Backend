package com.englishschool.courseservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/upload")
@Slf4j
@Tag(name = "Upload", description = "Image upload for course thumbnails")
public class UploadController {

    private static final String UPLOAD_DIR = "uploads";
    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024; // 200 MB
    private static final String[] ALLOWED_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp"};

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload course thumbnail image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file selected"));
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("error", "File too large (max 200 MB)"));
        }
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedType(contentType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid file type. Use JPEG, PNG, GIF or WebP"));
        }
        String ext = getExtension(contentType);
        String filename = UUID.randomUUID().toString() + ext;
        try {
            Path dir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            // Use path that works through Angular proxy (/uploads -> course-service:8083)
            String url = "/uploads/" + filename;
            Map<String, String> body = new HashMap<>();
            body.put("url", url);
            body.put("filename", filename);
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } catch (IOException e) {
            log.error("Failed to save upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to save file"));
        }
    }

    private boolean isAllowedType(String contentType) {
        for (String t : ALLOWED_TYPES) {
            if (t.equals(contentType)) return true;
        }
        return false;
    }

    private String getExtension(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
