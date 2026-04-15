package com.englishschool.courseservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSearchRequest {

    private String search;           // name or description
    private String level;            // A1, A2, B1, B2, C1, C2
    private Long categoryId;
    private Long instructorId;
    private Boolean isPublished;
    private Double minRating;
    private Boolean freeOnly;        // true = only free (price 0), false = only paid
    private String sortBy;           // courseId, name, price, ratingAvg, ratingCount
    private String sortDir;          // asc, desc
    private int page;
    private int size;
}
