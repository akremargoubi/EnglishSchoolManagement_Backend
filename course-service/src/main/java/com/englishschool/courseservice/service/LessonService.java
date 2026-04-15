package com.englishschool.courseservice.service;

import com.englishschool.courseservice.dto.LessonDTO;
import com.englishschool.courseservice.entity.Lesson;
import com.englishschool.courseservice.entity.Module;
import com.englishschool.courseservice.exception.ResourceNotFoundException;
import com.englishschool.courseservice.mapper.LessonMapper;
import com.englishschool.courseservice.repository.LessonRepository;
import com.englishschool.courseservice.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository repository;
    private final ModuleRepository moduleRepository;
    private final LessonMapper mapper;

    @Transactional
    public LessonDTO create(LessonDTO dto) {
        Module module = moduleRepository.findById(dto.getModuleId())
                .orElseThrow(() -> new ResourceNotFoundException("Module", dto.getModuleId()));
        Lesson l = mapper.toEntity(dto);
        l.setModule(module);
        return mapper.toDTO(repository.save(l));
    }

    public List<LessonDTO> getByModuleId(Long moduleId) {
        return repository.findByModule_IdOrderByOrderIndexAsc(moduleId).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public LessonDTO getById(Long id) {
        Lesson l = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", id));
        return mapper.toDTO(l);
    }

    @Transactional
    public LessonDTO update(Long id, LessonDTO dto) {
        Lesson l = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", id));
        l.setTitle(dto.getTitle());
        if (dto.getContentType() != null) l.setContentType(dto.getContentType());
        if (dto.getContentUrl() != null) l.setContentUrl(dto.getContentUrl());
        if (dto.getContentText() != null) l.setContentText(dto.getContentText());
        if (dto.getQuizContentJson() != null) l.setQuizContentJson(dto.getQuizContentJson());
        if (dto.getDurationMinutes() != null) l.setDurationMinutes(dto.getDurationMinutes());
        if (dto.getOrderIndex() != null) l.setOrderIndex(dto.getOrderIndex());
        return mapper.toDTO(repository.save(l));
    }

    public void delete(Long id) {
        if (!repository.existsById(id))
            throw new ResourceNotFoundException("Lesson", id);
        repository.deleteById(id);
    }
}
