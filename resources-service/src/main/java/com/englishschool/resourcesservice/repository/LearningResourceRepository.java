package com.englishschool.resourcesservice.repository;

import com.englishschool.resourcesservice.entity.LearningResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {

    List<LearningResource> findByAssessmentId(Long assessmentId);
}