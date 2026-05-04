package com.esprit.esmauthms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class StudentClassResponseDto {
    private Long id;
    private String name;
    private String level;
    private String specialty;
    private String description;
    private Instant createdAt;

    // Tutor summary
    private UUID tutorId;
    private String tutorFirstName;
    private String tutorLastName;
    private String tutorEmail;

    // Students summary
    private int studentCount;
    private List<StudentSummary> students;

    @Data
    @Builder
    public static class StudentSummary {
        private UUID id;
        private String firstName;
        private String lastName;
        private String email;
        private String cin;
        private String avatarUrl;
    }
}
