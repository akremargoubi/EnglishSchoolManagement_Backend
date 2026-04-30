package com.esprit.esmauthms.service;

import com.esprit.esmauthms.dto.StudentClassRequest;
import com.esprit.esmauthms.dto.StudentClassResponseDto;
import com.esprit.esmauthms.entity.StudentClass;
import com.esprit.esmauthms.entity.User;
import com.esprit.esmauthms.repository.StudentClassRepository;
import com.esprit.esmauthms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentClassService — class and student/tutor management")
class StudentClassServiceTest {

    @Mock StudentClassRepository classRepository;
    @Mock UserRepository userRepository;

    @InjectMocks StudentClassService classService;

    private StudentClass sampleClass;

    @BeforeEach
    void setUp() {
        sampleClass = new StudentClass();
        sampleClass.setId(1L);
        sampleClass.setName("EC1");
        sampleClass.setLevel("3ème année");
        sampleClass.setSpecialty("Informatique");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create — persists class and returns DTO")
    void create_persistsAndReturnsDto() {
        StudentClassRequest req = new StudentClassRequest();
        req.setName("EC1");
        req.setLevel("3ème année");
        req.setSpecialty("Informatique");

        when(classRepository.save(any(StudentClass.class))).thenReturn(sampleClass);

        StudentClassResponseDto result = classService.create(req);

        assertThat(result.getName()).isEqualTo("EC1");
        assertThat(result.getLevel()).isEqualTo("3ème année");
        verify(classRepository).save(any(StudentClass.class));
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll — returns list of all classes")
    void getAll_returnsAllClasses() {
        StudentClass second = new StudentClass();
        second.setId(2L);
        second.setName("EC2");

        when(classRepository.findAll()).thenReturn(List.of(sampleClass, second));

        List<StudentClassResponseDto> result = classService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(StudentClassResponseDto::getName)
                .containsExactly("EC1", "EC2");
    }

    @Test
    @DisplayName("getAll — empty repository returns empty list")
    void getAll_emptyRepo_returnsEmptyList() {
        when(classRepository.findAll()).thenReturn(List.of());
        assertThat(classService.getAll()).isEmpty();
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById — existing id returns DTO")
    void getById_existingId_returnsDto() {
        when(classRepository.findById(1L)).thenReturn(Optional.of(sampleClass));

        StudentClassResponseDto result = classService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("EC1");
    }

    @Test
    @DisplayName("getById — unknown id throws exception")
    void getById_unknownId_throwsException() {
        when(classRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> classService.getById(99L))
                .isInstanceOf(RuntimeException.class);
    }

    // ── assign tutor ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("assignTutor — links tutor to class")
    void assignTutor_linksTutorToClass() {
        UUID tutorId = UUID.randomUUID();
        User tutor = User.builder().id(tutorId).role("TUTOR").build();

        when(classRepository.findById(1L)).thenReturn(Optional.of(sampleClass));
        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(classRepository.save(any())).thenReturn(sampleClass);

        StudentClassResponseDto result = classService.assignTutor(1L, tutorId);

        assertThat(result).isNotNull();
        verify(classRepository).save(sampleClass);
    }

    // ── assign / remove student ───────────────────────────────────────────────

    @Test
    @DisplayName("assignStudent — enrolls student in class")
    void assignStudent_enrollsStudent() {
        UUID studentId = UUID.randomUUID();
        User student = User.builder().id(studentId).role("STUDENT").build();

        when(classRepository.findById(1L)).thenReturn(Optional.of(sampleClass));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(userRepository.save(any())).thenReturn(student);
        when(classRepository.save(any())).thenReturn(sampleClass);

        assertThatCode(() -> classService.assignStudent(1L, studentId))
                .doesNotThrowAnyException();
    }

    // ── getByTutor ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getClassesByTutor — returns classes for given tutor")
    void getClassesByTutor_returnsClasses() {
        UUID tutorId = UUID.randomUUID();
        when(classRepository.findByTutor_Id(tutorId)).thenReturn(List.of(sampleClass));

        List<StudentClassResponseDto> result = classService.getClassesByTutor(tutorId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("EC1");
    }
}
