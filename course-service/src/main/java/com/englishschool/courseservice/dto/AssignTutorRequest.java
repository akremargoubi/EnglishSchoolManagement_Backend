package com.englishschool.courseservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignTutorRequest {
    @NotBlank
    @Email
    private String tutorEmail;
}
