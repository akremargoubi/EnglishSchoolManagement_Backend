package com.esprit.esmauthms.dto;

import lombok.Data;

@Data
public class ParentUpdateRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
}
