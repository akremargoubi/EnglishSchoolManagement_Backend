package com.englishschool.resourcesservice.service;

import com.englishschool.resourcesservice.client.AuthServiceClient;
import com.englishschool.resourcesservice.entity.LearningResource;
import com.englishschool.resourcesservice.repository.LearningResourceRepository;
import com.englishschool.resourcesservice.security.CallerContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LearningResourceService {

    private final LearningResourceRepository repository;
    private final AuthServiceClient authClient;
    private final FileStorageService fileStorageService;

    public LearningResource upload(String title, String type, boolean published,
                                   Long assessmentId, MultipartFile file,
                                   CallerContext caller) {
        if (!caller.isAdmin() && !caller.isTutor()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only tutors or admins can upload resources");
        }

        if (!caller.isAdmin()) {
            String className = authClient.getAssessmentClassName(assessmentId);
            if (className == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found: " + assessmentId);
            }
            if (!authClient.isTutorOfClass(caller.userId(), className)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not the tutor of class: " + className);
            }
        }

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        try {
            String fileUrl = fileStorageService.store(file);

            return repository.save(LearningResource.builder()
                    .title(title)
                    .type(type)
                    .published(published)
                    .assessmentId(assessmentId)
                    .fileUrl(fileUrl)
                    .uploadedBy(caller.userId())
                    .build());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed: " + e.getMessage());
        }
    }

    public List<LearningResource> getByAssessmentId(Long assessmentId, CallerContext caller) {
        if (caller.isAdmin()) {
            return repository.findByAssessmentId(assessmentId);
        }

        String className = authClient.getAssessmentClassName(assessmentId);
        if (className == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found: " + assessmentId);
        }

        if (caller.isTutor()) {
            if (!authClient.isTutorOfClass(caller.userId(), className)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the tutor of this class");
            }
            return repository.findByAssessmentId(assessmentId);
        }

        if (caller.isStudent()) {
            if (!authClient.isStudentInClass(caller.userId(), className)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not enrolled in this class");
            }
            return repository.findByAssessmentIdAndPublished(assessmentId, true);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    public void delete(Long id, CallerContext caller) {
        if (caller.isAdmin()) {
            repository.deleteById(id);
            return;
        }
        repository.findByIdAndUploadedBy(id, caller.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You can only delete resources you uploaded"));
        repository.deleteById(id);
    }
}
