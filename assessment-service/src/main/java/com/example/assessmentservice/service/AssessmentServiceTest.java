package com.example.assessmentservice.service;

import com.example.assessmentservice.entity.Assessment;
import com.example.assessmentservice.entity.AssessmentStatus;
import com.example.assessmentservice.entity.AssessmentType;
import com.example.assessmentservice.repository.AssessmentRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssessmentService Tests")
class AssessmentServiceTest {

    // ── Mocks ──────────────────────────────────────────────────────────────────
    @Mock
    private AssessmentRepository repository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AssessmentService assessmentService;

    // ── Test data ──────────────────────────────────────────────────────────────
    private Assessment assessment1;
    private Assessment assessment2;

    @BeforeEach
    void setUp() {
        // Préparer les données de test avant chaque test
        assessment1 = Assessment.builder()
                .id(1L)
                .title("Midterm Grammar")
                .courseName("English B2")
                .type(AssessmentType.EXAM)
                .status(AssessmentStatus.PUBLISHED)
                .startDate(LocalDateTime.now().plusDays(2))
                .endDate(LocalDateTime.now().plusDays(2).plusHours(2))
                .duration(120)
                .build();

        assessment2 = Assessment.builder()
                .id(2L)
                .title("Weekly Quiz")
                .courseName("English A1")
                .type(AssessmentType.QUIZ)
                .status(AssessmentStatus.DRAFT)
                .startDate(LocalDateTime.now().plusDays(5))
                .endDate(LocalDateTime.now().plusDays(5).plusHours(1))
                .duration(60)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CREATE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("create() — should save and return assessment")
    void create_shouldSaveAndReturnAssessment() {
        // GIVEN
        when(repository.save(any(Assessment.class))).thenReturn(assessment1);

        // WHEN
        Assessment result = assessmentService.create(assessment1);

        // THEN
        assertNotNull(result);
        assertEquals("Midterm Grammar", result.getTitle());
        assertEquals("English B2", result.getCourseName());
        assertEquals(AssessmentType.EXAM, result.getType());
        verify(repository, times(1)).save(assessment1);
    }

    @Test
    @DisplayName("create() — should call repository.save exactly once")
    void create_shouldCallSaveOnce() {
        // GIVEN
        when(repository.save(any(Assessment.class))).thenReturn(assessment1);

        // WHEN
        assessmentService.create(assessment1);

        // THEN
        verify(repository, times(1)).save(any(Assessment.class));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET ALL
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getAll() — should return list of all assessments")
    void getAll_shouldReturnAllAssessments() {
        // GIVEN
        List<Assessment> assessments = Arrays.asList(assessment1, assessment2);
        when(repository.findAll()).thenReturn(assessments);

        // WHEN
        List<Assessment> result = assessmentService.getAll();

        // THEN
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Midterm Grammar", result.get(0).getTitle());
        assertEquals("Weekly Quiz", result.get(1).getTitle());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll() — should return empty list when no assessments")
    void getAll_shouldReturnEmptyList() {
        // GIVEN
        when(repository.findAll()).thenReturn(List.of());

        // WHEN
        List<Assessment> result = assessmentService.getAll();

        // THEN
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET BY ID
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getById() — should return assessment when found")
    void getById_shouldReturnAssessment_whenFound() {
        // GIVEN
        when(repository.findById(1L)).thenReturn(Optional.of(assessment1));

        // WHEN
        Assessment result = assessmentService.getById(1L);

        // THEN
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Midterm Grammar", result.getTitle());
    }

    @Test
    @DisplayName("getById() — should throw RuntimeException when not found")
    void getById_shouldThrowException_whenNotFound() {
        // GIVEN
        when(repository.findById(99L)).thenReturn(Optional.empty());

        // WHEN & THEN
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> assessmentService.getById(99L)
        );
        assertTrue(exception.getMessage().contains("99"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("update() — should update fields and return updated assessment")
    void update_shouldUpdateFieldsAndReturn() {
        // GIVEN
        Assessment updated = Assessment.builder()
                .title("Final Exam")
                .courseName("English C1")
                .type(AssessmentType.EXAM)
                .status(AssessmentStatus.PUBLISHED)
                .startDate(LocalDateTime.now().plusDays(10))
                .endDate(LocalDateTime.now().plusDays(10).plusHours(3))
                .duration(180)
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(assessment1));
        when(repository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        Assessment result = assessmentService.update(1L, updated);

        // THEN
        assertEquals("Final Exam", result.getTitle());
        assertEquals("English C1", result.getCourseName());
        assertEquals(180, result.getDuration());
        verify(repository, times(1)).save(any(Assessment.class));
    }

    @Test
    @DisplayName("update() — should throw exception when assessment not found")
    void update_shouldThrowException_whenNotFound() {
        // GIVEN
        when(repository.findById(99L)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(
                RuntimeException.class,
                () -> assessmentService.update(99L, assessment1)
        );
        verify(repository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("delete() — should call deleteById once")
    void delete_shouldCallDeleteById() {
        // GIVEN
        doNothing().when(repository).deleteById(1L);

        // WHEN
        assessmentService.delete(1L);

        // THEN
        verify(repository, times(1)).deleteById(1L);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PLANNING
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getUpcoming() — should return list of upcoming assessments")
    void getUpcoming_shouldReturnUpcomingAssessments() {
        // GIVEN
        List<Assessment> upcoming = List.of(assessment1, assessment2);
        when(repository.findUpcoming(any(LocalDateTime.class))).thenReturn(upcoming);

        // WHEN
        List<Assessment> result = assessmentService.getUpcoming();

        // THEN
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository, times(1)).findUpcoming(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("getOngoing() — should return list of ongoing assessments")
    void getOngoing_shouldReturnOngoingAssessments() {
        // GIVEN
        when(repository.findOngoing(any(LocalDateTime.class))).thenReturn(List.of(assessment1));

        // WHEN
        List<Assessment> result = assessmentService.getOngoing();

        // THEN
        assertEquals(1, result.size());
        assertEquals("Midterm Grammar", result.get(0).getTitle());
    }

    @Test
    @DisplayName("getByMonth() — should return assessments for given month")
    void getByMonth_shouldReturnAssessmentsForMonth() {
        // GIVEN
        when(repository.findByMonth(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(assessment1));

        // WHEN
        List<Assessment> result = assessmentService.getByMonth(2026, 4);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository, times(1)).findByMonth(any(), any());
    }

    @Test
    @DisplayName("getByDateRange() — should return assessments in date range")
    void getByDateRange_shouldReturnAssessmentsInRange() {
        // GIVEN
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end   = LocalDateTime.now().plusDays(7);
        when(repository.findByDateRange(eq(start), eq(end)))
                .thenReturn(List.of(assessment1, assessment2));

        // WHEN
        List<Assessment> result = assessmentService.getByDateRange(start, end);

        // THEN
        assertEquals(2, result.size());
        verify(repository, times(1)).findByDateRange(start, end);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ENTITY — isUpcoming / isOngoing
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Assessment.isUpcoming() — should return true when startDate is in future")
    void isUpcoming_shouldReturnTrue_whenFutureDate() {
        // GIVEN
        Assessment a = Assessment.builder()
                .startDate(LocalDateTime.now().plusHours(5))
                .build();

        // WHEN & THEN
        assertTrue(a.isUpcoming());
    }

    @Test
    @DisplayName("Assessment.isUpcoming() — should return false when startDate is in past")
    void isUpcoming_shouldReturnFalse_whenPastDate() {
        // GIVEN
        Assessment a = Assessment.builder()
                .startDate(LocalDateTime.now().minusDays(1))
                .build();

        // WHEN & THEN
        assertFalse(a.isUpcoming());
    }

    @Test
    @DisplayName("Assessment.isOngoing() — should return true when now is between start and end")
    void isOngoing_shouldReturnTrue_whenNowBetweenStartAndEnd() {
        // GIVEN
        Assessment a = Assessment.builder()
                .startDate(LocalDateTime.now().minusHours(1))
                .endDate(LocalDateTime.now().plusHours(1))
                .build();

        // WHEN & THEN
        assertTrue(a.isOngoing());
    }

    @Test
    @DisplayName("Assessment.isOngoing() — should return false when assessment not started yet")
    void isOngoing_shouldReturnFalse_whenNotStarted() {
        // GIVEN
        Assessment a = Assessment.builder()
                .startDate(LocalDateTime.now().plusHours(2))
                .endDate(LocalDateTime.now().plusHours(4))
                .build();

        // WHEN & THEN
        assertFalse(a.isOngoing());
    }
}