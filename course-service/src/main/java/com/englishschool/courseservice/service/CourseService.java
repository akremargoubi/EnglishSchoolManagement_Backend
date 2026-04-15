package com.englishschool.courseservice.service;

import com.englishschool.courseservice.dto.*;
import com.englishschool.courseservice.entity.Category;
import com.englishschool.courseservice.entity.Course;
import com.englishschool.courseservice.entity.Instructor;
import com.englishschool.courseservice.exception.ResourceNotFoundException;
import com.englishschool.courseservice.mapper.CourseMapper;
import com.englishschool.courseservice.repository.CourseRepository;
import com.englishschool.courseservice.repository.CourseSpecification;
import com.englishschool.courseservice.repository.CategoryRepository;
import com.englishschool.courseservice.repository.InstructorRepository;
import com.englishschool.courseservice.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository repository;
    private final CategoryRepository categoryRepository;
    private final InstructorRepository instructorRepository;
    private final CourseMapper mapper;

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_COURSES, CacheConfig.CACHE_COURSE_BY_ID}, allEntries = true)
    public CourseDTO create(CourseDTO dto) {
        Course c = mapper.toEntity(dto);
        if (dto.getCategoryId() != null) {
            Category cat = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", dto.getCategoryId()));
            c.setCategory(cat);
        }
        if (dto.getInstructorId() != null) {
            Instructor inst = instructorRepository.findById(dto.getInstructorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Instructor", dto.getInstructorId()));
            c.setInstructor(inst);
        }
        return mapper.toDTO(repository.save(c));
    }

    @Cacheable(value = CacheConfig.CACHE_COURSES, key = "#root.method.name")
    public List<CourseDTO> getAll() {
        return repository.findAll().stream().map(mapper::toDTO).collect(Collectors.toList());
    }

    @Cacheable(value = CacheConfig.CACHE_COURSE_BY_ID, key = "#id")
    public CourseDTO getById(Long id) {
        Course c = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
        return mapper.toDTO(c);
    }

    public PageResponse<CourseDTO> search(CourseSearchRequest req) {
        int page = Math.max(0, req.getPage());
        int size = req.getSize() <= 0 ? 20 : Math.min(100, req.getSize());
        Sort sort = buildSort(req.getSortBy(), req.getSortDir());
        Pageable pageable = PageRequest.of(page, size, sort);

        var spec = CourseSpecification.search(req);
        var pageResult = repository.findAll(spec, pageable);

        return PageResponse.<CourseDTO>builder()
                .content(pageResult.getContent().stream().map(mapper::toDTO).collect(Collectors.toList()))
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .build();
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_COURSES, CacheConfig.CACHE_COURSE_BY_ID}, allEntries = true)
    public CourseDTO update(Long id, CourseDTO dto) {
        Course c = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
        c.setName(dto.getName());
        c.setLevel(dto.getLevel());
        c.setDescription(dto.getDescription());
        c.setPrice(dto.getPrice());
        c.setThumbnailUrl(dto.getThumbnailUrl());
        if (dto.getIsPublished() != null) c.setIsPublished(dto.getIsPublished());
        if (dto.getCategoryId() != null) {
            Category cat = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", dto.getCategoryId()));
            c.setCategory(cat);
        } else {
            c.setCategory(null);
        }
        if (dto.getInstructorId() != null) {
            Instructor inst = instructorRepository.findById(dto.getInstructorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Instructor", dto.getInstructorId()));
            c.setInstructor(inst);
        } else {
            c.setInstructor(null);
        }
        return mapper.toDTO(repository.save(c));
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_COURSES, CacheConfig.CACHE_COURSE_BY_ID}, allEntries = true)
    public CourseDTO publish(Long id) {
        Course c = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
        c.setIsPublished(true);
        return mapper.toDTO(repository.save(c));
    }

    @CacheEvict(value = {CacheConfig.CACHE_COURSES, CacheConfig.CACHE_COURSE_BY_ID}, allEntries = true)
    public void delete(Long id) {
        if (!repository.existsById(id))
            throw new ResourceNotFoundException("Course", id);
        repository.deleteById(id);
    }

    private Sort buildSort(String sortBy, String sortDir) {
        if (sortBy == null || sortBy.isBlank()) sortBy = "courseId";
        Sort.Direction dir = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, sortBy);
    }
}
