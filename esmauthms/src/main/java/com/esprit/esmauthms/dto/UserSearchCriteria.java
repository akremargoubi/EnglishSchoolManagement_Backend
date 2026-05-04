package com.esprit.esmauthms.dto;

import lombok.Data;

@Data
public class UserSearchCriteria {

    private String email;
    private String firstName;
    private String lastName;
    private String cin;
    private String phoneNumber;
    private String role;
}
