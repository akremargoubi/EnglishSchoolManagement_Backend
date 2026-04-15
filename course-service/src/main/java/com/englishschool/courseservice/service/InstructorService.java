package com.englishschool.courseservice.service;

import com.englishschool.courseservice.dto.InstructorDTO;
import com.englishschool.courseservice.entity.Instructor;
import com.englishschool.courseservice.exception.ResourceNotFoundException;
import com.englishschool.courseservice.mapper.InstructorMapper;
import com.englishschool.courseservice.repository.InstructorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstructorService {

    private final InstructorRepository repository;
    private final InstructorMapper mapper;

    @Transactional
    public InstructorDTO create(InstructorDTO dto) {
        Instructor i = mapper.toEntity(dto);
        return mapper.toDTO(repository.save(i));
    }

    @Transactional(readOnly = true)
    public List<InstructorDTO> getAll() {
        return repository.findAll().stream()
                .map(mapper::toDTO)
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InstructorDTO getById(Long id) {
        Instructor i = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor", id));
        return mapper.toDTO(i);
    }

    @Transactional
    public InstructorDTO update(Long id, InstructorDTO dto) {
        Instructor i = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor", id));
        i.setFirstName(dto.getFirstName());
        i.setLastName(dto.getLastName());
        i.setEmail(dto.getEmail());
        i.setBio(dto.getBio());
        i.setAvatarUrl(dto.getAvatarUrl());
        return mapper.toDTO(repository.save(i));
    }

    public void delete(Long id) {
        if (!repository.existsById(id))
            throw new ResourceNotFoundException("Instructor", id);
        repository.deleteById(id);
    }
}
