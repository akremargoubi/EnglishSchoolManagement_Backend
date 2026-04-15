package com.englishschool.courseservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorDTO {

    private Long id;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @Email
    @Size(max = 255)
    private String email;

    private String bio;

    @Size(max = 500)
    private String avatarUrl;
}
