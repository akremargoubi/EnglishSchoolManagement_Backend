package com.englishschool.courseservice.repository;

import com.englishschool.courseservice.dto.CourseSearchRequest;
import com.englishschool.courseservice.entity.Course;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class CourseSpecification {

    private CourseSpecification() {}

    public static Specification<Course> search(CourseSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (req.getSearch() != null && !req.getSearch().isBlank()) {
                String pattern = "%" + req.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            if (req.getLevel() != null && !req.getLevel().isBlank()) {
                predicates.add(cb.equal(root.get("level"), req.getLevel()));
            }
            if (req.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), req.getCategoryId()));
            }
            if (req.getInstructorId() != null) {
                predicates.add(cb.equal(root.get("instructor").get("id"), req.getInstructorId()));
            }
            if (req.getIsPublished() != null) {
                predicates.add(cb.equal(root.get("isPublished"), req.getIsPublished()));
            }
            if (req.getMinRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("ratingAvg"), req.getMinRating()));
            }
            if (req.getFreeOnly() != null && req.getFreeOnly()) {
                predicates.add(cb.or(
                        cb.isNull(root.get("price")),
                        cb.equal(root.get("price"), 0)
                ));
            } else if (req.getFreeOnly() != null && !req.getFreeOnly()) {
                predicates.add(cb.greaterThan(root.get("price"), 0));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
