package com.englishschool.resourcesservice.service;

import com.englishschool.resourcesservice.client.AuthServiceClient;
import com.englishschool.resourcesservice.entity.LearningResource;
import com.englishschool.resourcesservice.repository.LearningResourceRepository;
import com.englishschool.resourcesservice.security.CallerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LearningResourceService Tests")
class LearningResourceServiceTest {

    @Mock
    private LearningResourceRepository repository;

    @Mock
    private AuthServiceClient authClient;

    @InjectMocks
    private LearningResourceService service;

    // Admin caller bypasses all auth checks — simplest for unit tests
    private static final CallerContext ADMIN = new CallerContext(UUID.randomUUID(), "ADMIN", "admin@test.com");

    private LearningResource resource1;
    private LearningResource resource2;

    @BeforeEach
    void setUp() {
        resource1 = new LearningResource();
        resource1.setId(1L);
        resource1.setTitle("Grammar Guide");
        resource1.setType("PDF");
        resource1.setPublished(true);
        resource1.setAssessmentId(10L);
        resource1.setFileUrl("uploads/grammar_guide.pdf");

        resource2 = new LearningResource();
        resource2.setId(2L);
        resource2.setTitle("Vocabulary Video");
        resource2.setType("VIDEO");
        resource2.setPublished(false);
        resource2.setAssessmentId(10L);
        resource2.setFileUrl("uploads/vocabulary_video.mp4");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET BY ASSESSMENT ID
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getByAssessmentId() — should return resources for given assessment")
    void getByAssessmentId_shouldReturnResources() {
        List<LearningResource> resources = Arrays.asList(resource1, resource2);
        when(repository.findByAssessmentId(10L)).thenReturn(resources);

        List<LearningResource> result = service.getByAssessmentId(10L, ADMIN);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Grammar Guide", result.get(0).getTitle());
        assertEquals("Vocabulary Video", result.get(1).getTitle());
        verify(repository, times(1)).findByAssessmentId(10L);
    }

    @Test
    @DisplayName("getByAssessmentId() — should return empty list when no resources")
    void getByAssessmentId_shouldReturnEmptyList_whenNoResources() {
        when(repository.findByAssessmentId(99L)).thenReturn(List.of());

        List<LearningResource> result = service.getByAssessmentId(99L, ADMIN);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findByAssessmentId(99L);
    }

    @Test
    @DisplayName("getByAssessmentId() — should return only resources for that assessment")
    void getByAssessmentId_shouldFilterByAssessment() {
        when(repository.findByAssessmentId(10L)).thenReturn(List.of(resource1));
        when(repository.findByAssessmentId(20L)).thenReturn(List.of(resource2));

        List<LearningResource> result10 = service.getByAssessmentId(10L, ADMIN);
        List<LearningResource> result20 = service.getByAssessmentId(20L, ADMIN);

        assertEquals(1, result10.size());
        assertEquals("Grammar Guide", result10.get(0).getTitle());
        assertEquals(1, result20.size());
        assertEquals("Vocabulary Video", result20.get(0).getTitle());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("delete() — should call deleteById once with correct id")
    void delete_shouldCallDeleteById() {
        doNothing().when(repository).deleteById(1L);

        service.delete(1L, ADMIN);

        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("delete() — should never call deleteById with wrong id")
    void delete_shouldNotCallDeleteWithWrongId() {
        doNothing().when(repository).deleteById(1L);

        service.delete(1L, ADMIN);

        verify(repository, never()).deleteById(2L);
        verify(repository, never()).deleteById(99L);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UPLOAD — validation
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("upload() — should throw exception when file is null")
    void upload_shouldThrowException_whenFileIsNull() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.upload("Grammar Guide", "PDF", true, 10L, null, ADMIN)
        );
        assertNotNull(exception.getMessage());
    }

    @Test
    @DisplayName("upload() — should throw exception when file is empty")
    void upload_shouldThrowException_whenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new byte[0]
        );

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.upload("Grammar Guide", "PDF", true, 10L, emptyFile, ADMIN)
        );
        assertNotNull(exception.getMessage());
    }

    @Test
    @DisplayName("upload() — should save resource when file is valid")
    void upload_shouldSaveResource_whenFileIsValid() {
        MockMultipartFile validFile = new MockMultipartFile(
                "file", "grammar.pdf", "application/pdf", "PDF content here".getBytes()
        );
        when(repository.save(any(LearningResource.class))).thenReturn(resource1);

        assertDoesNotThrow(() ->
                service.upload("Grammar Guide", "PDF", true, 10L, validFile, ADMIN)
        );

        verify(repository, times(1)).save(any(LearningResource.class));
    }

    @Test
    @DisplayName("upload() — should set correct title and type on saved resource")
    void upload_shouldSetCorrectFields_onSavedResource() {
        MockMultipartFile validFile = new MockMultipartFile(
                "file", "vocab.pdf", "application/pdf", "some content".getBytes()
        );
        when(repository.save(any(LearningResource.class))).thenAnswer(invocation -> {
            LearningResource saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        service.upload("Vocabulary PDF", "PDF", false, 5L, validFile, ADMIN);

        verify(repository, times(1)).save(argThat(r ->
                r.getTitle().equals("Vocabulary PDF") &&
                        r.getType().equals("PDF") &&
                        r.getAssessmentId().equals(5L) &&
                        !r.isPublished()
        ));
    }

    @Test
    @DisplayName("upload() — fileUrl should start with 'uploads/'")
    void upload_shouldSetFileUrlStartingWithUploads() {
        MockMultipartFile validFile = new MockMultipartFile(
                "file", "notes.pdf", "application/pdf", "notes content".getBytes()
        );
        when(repository.save(any(LearningResource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.upload("Notes", "PDF", true, 10L, validFile, ADMIN);

        verify(repository, times(1)).save(argThat(r ->
                r.getFileUrl() != null && r.getFileUrl().startsWith("uploads/")
        ));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ENTITY — LearningResource
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("LearningResource — getters and setters should work correctly")
    void learningResource_gettersAndSetters_shouldWork() {
        LearningResource r = new LearningResource();
        r.setId(5L);
        r.setTitle("Test Resource");
        r.setType("PDF");
        r.setPublished(true);
        r.setAssessmentId(20L);
        r.setFileUrl("uploads/test.pdf");

        assertEquals(5L, r.getId());
        assertEquals("Test Resource", r.getTitle());
        assertEquals("PDF", r.getType());
        assertTrue(r.isPublished());
        assertEquals(20L, r.getAssessmentId());
        assertEquals("uploads/test.pdf", r.getFileUrl());
    }

    @Test
    @DisplayName("LearningResource — published flag should default to false")
    void learningResource_publishedFlag_shouldDefaultToFalse() {
        LearningResource r = new LearningResource();
        assertFalse(r.isPublished());
    }
}
