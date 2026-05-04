package com.example.assessmentservice.service;

import com.example.assessmentservice.client.AuthServiceClient;
import com.example.assessmentservice.entity.Assessment;
import com.example.assessmentservice.entity.AssessmentStatus;
import com.example.assessmentservice.entity.AssessmentType;
import com.example.assessmentservice.repository.AssessmentRepository;
import com.example.assessmentservice.security.CallerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssessmentService Tests")
class AssessmentServiceTest {

    @Mock
    private AssessmentRepository repository;

    @Mock
    private AuthServiceClient authClient;

    @InjectMocks
    private AssessmentService assessmentService;

    // Admin caller bypasses all auth checks
    private static final CallerContext ADMIN = new CallerContext(UUID.randomUUID(), "ADMIN", "admin@test.com");

    private Assessment assessment1;
    private Assessment assessment2;

    @BeforeEach
    void setUp() {
        assessment1 = Assessment.builder()
                .id(1L)
                .title("Midterm Grammar")
                .courseName("English B2")
                .type(AssessmentType.EXAM)
                .status(AssessmentStatus.PUBLISHED)
                .className("TWIN1")
                .startDate("2026-06-01")
                .endDate("2026-06-01")
                .duration(120)
                .build();

        assessment2 = Assessment.builder()
                .id(2L)
                .title("Weekly Quiz")
                .courseName("English A1")
                .type(AssessmentType.QUIZ)
                .status(AssessmentStatus.DRAFT)
                .className("DS3")
                .startDate("2026-06-05")
                .endDate("2026-06-05")
                .duration(60)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CREATE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("create() — admin should save and return assessment")
    void create_shouldSaveAndReturnAssessment() {
        when(repository.save(any(Assessment.class))).thenReturn(assessment1);

        Assessment result = assessmentService.create(assessment1, ADMIN);

        assertNotNull(result);
        assertEquals("Midterm Grammar", result.getTitle());
        assertEquals("English B2", result.getCourseName());
        assertEquals(AssessmentType.EXAM, result.getType());
        verify(repository, times(1)).save(assessment1);
    }

    @Test
    @DisplayName("create() — should call repository.save exactly once")
    void create_shouldCallSaveOnce() {
        when(repository.save(any(Assessment.class))).thenReturn(assessment1);

        assessmentService.create(assessment1, ADMIN);

        verify(repository, times(1)).save(any(Assessment.class));
    }

    @Test
    @DisplayName("create() — student should be forbidden")
    void create_studentShouldBeForbidden() {
        CallerContext student = new CallerContext(UUID.randomUUID(), "USER", "student@test.com");

        assertThrows(RuntimeException.class, () ->
                assessmentService.create(assessment1, student));

        verify(repository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET ALL
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getAll() — admin should return all assessments")
    void getAll_adminShouldReturnAll() {
        List<Assessment> assessments = Arrays.asList(assessment1, assessment2);
        when(repository.findAll()).thenReturn(assessments);

        List<Assessment> result = assessmentService.getAll(ADMIN);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll() — admin should return empty list when no assessments")
    void getAll_adminShouldReturnEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<Assessment> result = assessmentService.getAll(ADMIN);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getAll() — student should be forbidden")
    void getAll_studentShouldBeForbidden() {
        CallerContext student = new CallerContext(UUID.randomUUID(), "USER", "student@test.com");

        assertThrows(RuntimeException.class, () -> assessmentService.getAll(student));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET BY ID
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getById() — should return assessment when found")
    void getById_shouldReturnAssessment_whenFound() {
        when(repository.findById(1L)).thenReturn(Optional.of(assessment1));

        Assessment result = assessmentService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Midterm Grammar", result.getTitle());
    }

    @Test
    @DisplayName("getById() — should throw exception when not found")
    void getById_shouldThrowException_whenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> assessmentService.getById(99L));
        assertTrue(ex.getMessage().contains("99"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("update() — admin should update fields and return updated assessment")
    void update_adminShouldUpdateFields() {
        Assessment updated = Assessment.builder()
                .title("Final Exam")
                .courseName("English C1")
                .type(AssessmentType.EXAM)
                .status(AssessmentStatus.PUBLISHED)
                .startDate("2026-07-10")
                .endDate("2026-07-10")
                .duration(180)
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(assessment1));
        when(repository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));

        Assessment result = assessmentService.update(1L, updated, ADMIN);

        assertEquals("Final Exam", result.getTitle());
        assertEquals("English C1", result.getCourseName());
        assertEquals(180, result.getDuration());
        verify(repository, times(1)).save(any(Assessment.class));
    }

    @Test
    @DisplayName("update() — should throw exception when assessment not found")
    void update_shouldThrowException_whenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> assessmentService.update(99L, assessment1, ADMIN));
        verify(repository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("delete() — admin should call deleteById once")
    void delete_adminShouldCallDeleteById() {
        when(repository.findById(1L)).thenReturn(Optional.of(assessment1));
        doNothing().when(repository).deleteById(1L);

        assessmentService.delete(1L, ADMIN);

        verify(repository, times(1)).deleteById(1L);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PLANNING (fixed — repository uses String dates)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getUpcoming() — should return upcoming assessments")
    void getUpcoming_shouldReturnAssessments() {
        List<Assessment> upcoming = List.of(assessment1, assessment2);
        when(repository.findUpcoming(any(String.class))).thenReturn(upcoming);

        List<Assessment> result = assessmentService.getUpcoming();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository, times(1)).findUpcoming(any(String.class));
    }

    @Test
    @DisplayName("getOngoing() — should return ongoing assessments")
    void getOngoing_shouldReturnAssessments() {
        when(repository.findOngoing(any(String.class))).thenReturn(List.of(assessment1));

        List<Assessment> result = assessmentService.getOngoing();

        assertEquals(1, result.size());
        assertEquals("Midterm Grammar", result.get(0).getTitle());
        verify(repository, times(1)).findOngoing(any(String.class));
    }

    @Test
    @DisplayName("getByMonth() — should return assessments for given month")
    void getByMonth_shouldReturnAssessmentsForMonth() {
        when(repository.findByYearMonth(any(String.class))).thenReturn(List.of(assessment1));

        List<Assessment> result = assessmentService.getByMonth(2026, 6);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository, times(1)).findByYearMonth("2026-06");
    }

    @Test
    @DisplayName("getByDateRange() — should return assessments in date range")
    void getByDateRange_shouldReturnAssessmentsInRange() {
        when(repository.findByDateRange(any(String.class), any(String.class)))
                .thenReturn(List.of(assessment1, assessment2));

        List<Assessment> result = assessmentService.getByDateRange(
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 6, 30, 23, 59));

        assertEquals(2, result.size());
        verify(repository, times(1)).findByDateRange(any(String.class), any(String.class));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ENTITY — field integrity
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Assessment — builder should set all fields correctly")
    void assessment_builderSetsFields() {
        assertEquals(1L,                          assessment1.getId());
        assertEquals("Midterm Grammar",            assessment1.getTitle());
        assertEquals("English B2",                assessment1.getCourseName());
        assertEquals(AssessmentType.EXAM,         assessment1.getType());
        assertEquals(AssessmentStatus.PUBLISHED,  assessment1.getStatus());
        assertEquals("TWIN1",                     assessment1.getClassName());
        assertEquals("2026-06-01",                assessment1.getStartDate());
        assertEquals(120,                         assessment1.getDuration());
    }

    @Test
    @DisplayName("Assessment — tutorId should be null by default")
    void assessment_tutorIdShouldBeNullByDefault() {
        Assessment a = new Assessment();
        assertNull(a.getTutorId());
    }
}
