package com.englishschool.courseservice.mapper;

import com.englishschool.courseservice.dto.LessonDTO;
import com.englishschool.courseservice.entity.Lesson;
import org.springframework.stereotype.Component;

@Component
public class LessonMapper {

    public LessonDTO toDTO(Lesson e) {
        if (e == null) return null;
        return LessonDTO.builder()
                .id(e.getId())
                .moduleId(e.getModule() != null ? e.getModule().getId() : null)
                .title(e.getTitle())
                .contentType(e.getContentType())
                .contentUrl(e.getContentUrl())
                .contentText(e.getContentText())
                .quizContentJson(e.getQuizContentJson())
                .durationMinutes(e.getDurationMinutes())
                .orderIndex(e.getOrderIndex())
                .build();
    }

    public Lesson toEntity(LessonDTO dto) {
        if (dto == null) return null;
        Lesson l = new Lesson();
        l.setId(dto.getId());
        l.setTitle(dto.getTitle());
        l.setContentType(dto.getContentType());
        l.setContentUrl(dto.getContentUrl());
        l.setContentText(dto.getContentText());
        l.setQuizContentJson(dto.getQuizContentJson());
        l.setDurationMinutes(dto.getDurationMinutes() != null ? dto.getDurationMinutes() : 0);
        l.setOrderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : 0);
        return l;
    }
}
