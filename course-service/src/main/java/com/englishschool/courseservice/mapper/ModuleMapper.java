package com.englishschool.courseservice.mapper;

import com.englishschool.courseservice.dto.LessonDTO;
import com.englishschool.courseservice.dto.ModuleDTO;
import com.englishschool.courseservice.entity.Lesson;
import com.englishschool.courseservice.entity.Module;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ModuleMapper {

    private final LessonMapper lessonMapper;

    public ModuleMapper(LessonMapper lessonMapper) {
        this.lessonMapper = lessonMapper;
    }

    public ModuleDTO toDTO(Module e, boolean includeLessons) {
        if (e == null) return null;
        List<LessonDTO> lessons = includeLessons && e.getLessons() != null
                ? e.getLessons().stream().map(lessonMapper::toDTO).collect(Collectors.toList())
                : null;
        return ModuleDTO.builder()
                .id(e.getId())
                .courseId(e.getCourse() != null ? e.getCourse().getCourseId() : null)
                .title(e.getTitle())
                .orderIndex(e.getOrderIndex())
                .lessons(lessons)
                .build();
    }

    public ModuleDTO toDTO(Module e) {
        return toDTO(e, false);
    }

    public Module toEntity(ModuleDTO dto) {
        if (dto == null) return null;
        Module m = new Module();
        m.setId(dto.getId());
        m.setTitle(dto.getTitle());
        m.setOrderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : 0);
        return m;
    }
}
