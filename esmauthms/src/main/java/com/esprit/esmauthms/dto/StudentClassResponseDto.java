package com.esprit.esmauthms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class StudentClassResponseDto {
    private Long id;
    private String name;
    private String level;
    private String specialty;
    private String description;
    private Instant createdAt;
    private int studentCount;
}