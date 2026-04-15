package com.englishschool.courseservice.mapper;

import com.englishschool.courseservice.dto.InstructorDTO;
import com.englishschool.courseservice.entity.Instructor;
import org.springframework.stereotype.Component;

@Component
public class InstructorMapper {

    public InstructorDTO toDTO(Instructor e) {
        if (e == null) return null;
        return InstructorDTO.builder()
                .id(e.getId())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .bio(e.getBio())
                .avatarUrl(e.getAvatarUrl())
                .build();
    }

    public Instructor toEntity(InstructorDTO dto) {
        if (dto == null) return null;
        Instructor i = new Instructor();
        i.setId(dto.getId());
        i.setFirstName(dto.getFirstName());
        i.setLastName(dto.getLastName());
        i.setEmail(dto.getEmail());
        i.setBio(dto.getBio());
        i.setAvatarUrl(dto.getAvatarUrl());
        return i;
    }
}
