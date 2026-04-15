package com.englishschool.courseservice.service;

import com.englishschool.courseservice.dto.CategoryDTO;
import com.englishschool.courseservice.entity.Category;
import com.englishschool.courseservice.exception.ResourceNotFoundException;
import com.englishschool.courseservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryDTO create(CategoryDTO dto) {
        Category c = Category.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .slug(dto.getSlug() != null ? dto.getSlug() : dto.getName().toLowerCase().replace(" ", "-"))
                .build();
        return toDTO(repository.save(c));
    }

    public List<CategoryDTO> getAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public CategoryDTO getById(Long id) {
        Category c = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return toDTO(c);
    }

    public CategoryDTO update(Long id, CategoryDTO dto) {
        Category c = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        c.setName(dto.getName());
        c.setDescription(dto.getDescription());
        if (dto.getSlug() != null) c.setSlug(dto.getSlug());
        return toDTO(repository.save(c));
    }

    public void delete(Long id) {
        if (!repository.existsById(id))
            throw new ResourceNotFoundException("Category", id);
        repository.deleteById(id);
    }

    private CategoryDTO toDTO(Category e) {
        return CategoryDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .slug(e.getSlug())
                .build();
    }
}
