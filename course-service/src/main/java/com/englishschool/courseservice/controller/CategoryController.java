package com.englishschool.courseservice.controller;

import com.englishschool.courseservice.dto.CategoryDTO;
import com.englishschool.courseservice.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category CRUD API")
public class CategoryController {

    private final CategoryService service;

    @GetMapping
    @Operation(summary = "Get all categories")
    public List<CategoryDTO> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public CategoryDTO getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create category")
    public CategoryDTO create(@Valid @RequestBody CategoryDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category")
    public CategoryDTO update(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete category")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
