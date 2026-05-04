package com.englishschool.resourcesservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String type;
    private boolean published;
    private Long assessmentId;
    private String fileUrl;

    // UUID of the tutor who uploaded this resource — used for ownership checks
    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "uploaded_at", updatable = false)
    private Instant uploadedAt;

    @PrePersist
    public void prePersist() {
        if (uploadedAt == null) uploadedAt = Instant.now();
    }
}
