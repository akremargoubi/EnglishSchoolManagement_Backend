package com.englishschool.resourcesservice.repository;

import com.englishschool.resourcesservice.entity.LearningResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {

    List<LearningResource> findByAssessmentId(Long assessmentId);

    List<LearningResource> findByAssessmentIdAndPublished(Long assessmentId, boolean published);

    Optional<LearningResource> findByIdAndUploadedBy(Long id, UUID uploadedBy);
}