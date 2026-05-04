package com.esprit.esmauthms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ParentLinkRequest {
    @NotBlank
    @Email
    private String parentEmail;
}
