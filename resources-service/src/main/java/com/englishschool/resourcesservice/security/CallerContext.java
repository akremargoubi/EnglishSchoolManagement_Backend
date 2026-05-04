package com.englishschool.resourcesservice.security;

import java.util.UUID;

public record CallerContext(UUID userId, String role, String email) {
    public boolean isAdmin()  { return "ADMIN".equalsIgnoreCase(role); }
    public boolean isTutor()  { return "TUTOR".equalsIgnoreCase(role); }
    public boolean isStudent(){ return "STUDENT".equalsIgnoreCase(role); }
}
