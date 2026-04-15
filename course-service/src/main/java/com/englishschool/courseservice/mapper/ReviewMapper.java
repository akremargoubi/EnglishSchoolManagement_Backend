package com.englishschool.courseservice.mapper;

import com.englishschool.courseservice.dto.ReviewDTO;
import com.englishschool.courseservice.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewDTO toDTO(Review e) {
        if (e == null) return null;
        return ReviewDTO.builder()
                .id(e.getId())
                .courseId(e.getCourse() != null ? e.getCourse().getCourseId() : null)
                .userId(e.getUserId())
                .rating(e.getRating())
                .comment(e.getComment())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public Review toEntity(ReviewDTO dto) {
        if (dto == null) return null;
        Review r = new Review();
        r.setId(dto.getId());
        r.setUserId(dto.getUserId());
        r.setRating(dto.getRating());
        r.setComment(dto.getComment());
        return r;
    }
}
