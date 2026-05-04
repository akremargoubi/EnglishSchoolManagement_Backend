package com.esprit.esmauthms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class StudentClassRequest {

    @NotBlank
    private String name;        // ex: "TWIN1"

    private String level;       // ex: "3ème année"

    private String specialty;   // ex: "Informatique"

    private String description;

    // Optional: assign a tutor at creation time
    private UUID tutorId;
}
