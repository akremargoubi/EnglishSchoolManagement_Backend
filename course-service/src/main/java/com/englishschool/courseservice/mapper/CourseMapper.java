package com.englishschool.courseservice.mapper;

import com.englishschool.courseservice.dto.CourseDTO;
import com.englishschool.courseservice.entity.Course;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    public CourseDTO toDTO(Course e) {
        if (e == null) return null;
        return CourseDTO.builder()
                .courseId(e.getCourseId())
                .name(e.getName())
                .level(e.getLevel())
                .description(e.getDescription())
                .categoryId(e.getCategory() != null ? e.getCategory().getId() : null)
                .instructorId(e.getInstructor() != null ? e.getInstructor().getId() : null)
                .price(e.getPrice())
                .thumbnailUrl(e.getThumbnailUrl())
                .isPublished(e.getIsPublished())
                .ratingAvg(e.getRatingAvg())
                .ratingCount(e.getRatingCount())
                .build();
    }

    public Course toEntity(CourseDTO dto) {
        if (dto == null) return null;
        Course c = new Course();
        c.setCourseId(dto.getCourseId());
        c.setName(dto.getName());
        c.setLevel(dto.getLevel());
        c.setDescription(dto.getDescription());
        c.setPrice(dto.getPrice());
        c.setThumbnailUrl(dto.getThumbnailUrl());
        c.setIsPublished(dto.getIsPublished() != null ? dto.getIsPublished() : false);
        // instructor set in service
        c.setRatingAvg(dto.getRatingAvg());
        c.setRatingCount(dto.getRatingCount() != null ? dto.getRatingCount() : 0);
        return c;
    }
}
