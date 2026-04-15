package com.englishschool.courseservice.service;

import com.englishschool.courseservice.dto.ModuleDTO;
import com.englishschool.courseservice.entity.Course;
import com.englishschool.courseservice.entity.Module;
import com.englishschool.courseservice.exception.ResourceNotFoundException;
import com.englishschool.courseservice.mapper.ModuleMapper;
import com.englishschool.courseservice.repository.CourseRepository;
import com.englishschool.courseservice.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleRepository repository;
    private final CourseRepository courseRepository;
    private final ModuleMapper mapper;

    @Transactional
    public ModuleDTO create(ModuleDTO dto) {
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", dto.getCourseId()));
        Module m = mapper.toEntity(dto);
        m.setCourse(course);
        return mapper.toDTO(repository.save(m));
    }

    public List<ModuleDTO> getByCourseId(Long courseId, boolean includeLessons) {
        return repository.findByCourseCourseIdOrderByOrderIndexAsc(courseId).stream()
                .map(m -> mapper.toDTO(m, includeLessons))
                .collect(Collectors.toList());
    }

    public ModuleDTO getById(Long id, boolean includeLessons) {
        Module m = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module", id));
        return mapper.toDTO(m, includeLessons);
    }

    @Transactional
    public ModuleDTO update(Long id, ModuleDTO dto) {
        Module m = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module", id));
        m.setTitle(dto.getTitle());
        if (dto.getOrderIndex() != null) m.setOrderIndex(dto.getOrderIndex());
        return mapper.toDTO(repository.save(m));
    }

    public void delete(Long id) {
        if (!repository.existsById(id))
            throw new ResourceNotFoundException("Module", id);
        repository.deleteById(id);
    }
}
