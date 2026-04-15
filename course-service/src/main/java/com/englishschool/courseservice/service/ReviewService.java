package com.englishschool.courseservice.service;

import com.englishschool.courseservice.dto.PageResponse;
import com.englishschool.courseservice.dto.ReviewDTO;
import com.englishschool.courseservice.entity.Course;
import com.englishschool.courseservice.entity.Review;
import com.englishschool.courseservice.exception.ResourceNotFoundException;
import com.englishschool.courseservice.mapper.ReviewMapper;
import com.englishschool.courseservice.repository.CourseRepository;
import com.englishschool.courseservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository repository;
    private final CourseRepository courseRepository;
    private final ReviewMapper mapper;

    @Transactional
    public ReviewDTO create(ReviewDTO dto) {
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", dto.getCourseId()));
        if (repository.existsByCourseCourseIdAndUserId(course.getCourseId(), dto.getUserId())) {
            throw new IllegalArgumentException("User has already reviewed this course");
        }
        Review r = mapper.toEntity(dto);
        r.setCourse(course);
        Review saved = repository.save(r);
        recalculateCourseRating(course);
        return mapper.toDTO(saved);
    }

    public PageResponse<ReviewDTO> getByCourseId(Long courseId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), size <= 0 ? 20 : Math.min(100, size),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> p = repository.findByCourseCourseId(courseId, pageable);
        return PageResponse.<ReviewDTO>builder()
                .content(p.getContent().stream().map(mapper::toDTO).collect(Collectors.toList()))
                .page(p.getNumber())
                .size(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .first(p.isFirst())
                .last(p.isLast())
                .build();
    }

    public ReviewDTO getById(Long id) {
        Review r = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", id));
        return mapper.toDTO(r);
    }

    @Transactional
    public ReviewDTO update(Long id, ReviewDTO dto) {
        Review r = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", id));
        r.setRating(dto.getRating());
        r.setComment(dto.getComment());
        Review saved = repository.save(r);
        recalculateCourseRating(r.getCourse());
        return mapper.toDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        Review r = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", id));
        Course course = r.getCourse();
        repository.delete(r);
        recalculateCourseRating(course);
    }

    private void recalculateCourseRating(Course course) {
        List<Review> reviews = repository.findByCourseCourseId(course.getCourseId());
        if (reviews.isEmpty()) {
            course.setRatingAvg(null);
            course.setRatingCount(0);
        } else {
            double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
            course.setRatingAvg(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            course.setRatingCount(reviews.size());
        }
        courseRepository.save(course);
    }
}
