package com.esprit.notificationms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailRequest {

    @Email
    @NotBlank
    private String to;

    @NotBlank
    private String subject;

    @NotBlank
    private String text;
}