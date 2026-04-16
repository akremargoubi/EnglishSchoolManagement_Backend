package com.esprit.esmauthms.dto;

import lombok.Data;

@Data
public class StudentClassRequest {
    private String name;        // ex: "TWIN1"
    private String level;       // ex: "3ème année"
    private String specialty;   // ex: "Informatique"
    private String description;
}