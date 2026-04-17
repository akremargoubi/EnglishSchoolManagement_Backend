package com.esprit.esmauthms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;
    private String firstName;
    private String lastName;
    private String cin;
    private String phoneNumber;
    private String address;
    private String role;
}