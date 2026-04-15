package com.englishschool.resourcesservice.service;

import com.englishschool.resourcesservice.entity.LearningResource;
import com.englishschool.resourcesservice.repository.LearningResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@Service
public class LearningResourceService {

    private final LearningResourceRepository repository;

    public LearningResourceService(LearningResourceRepository repository) {
        this.repository = repository;
    }

    // ==========================
    // UPLOAD
    // ==========================
    public void upload(String title,
                       String type,
                       boolean published,
                       Long assessmentId,
                       MultipartFile file) {

        try {

            if (file == null || file.isEmpty()) {
                throw new RuntimeException("File is empty");
            }

            // ✅ Chemin corrigé
            String uploadDir = new File("uploads").getAbsolutePath();
            File dir = new File(uploadDir);

            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            File destination = new File(dir, fileName);

            file.transferTo(destination);

            LearningResource resource = new LearningResource();
            resource.setTitle(title);
            resource.setType(type);
            resource.setPublished(published);
            resource.setAssessmentId(assessmentId);
            resource.setFileUrl("uploads/" + fileName);

            repository.save(resource);

            System.out.println("UPLOAD SUCCESS");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("File upload failed", e);
        }
    }

    // ==========================
    // GET BY ASSESSMENT
    // ==========================
    public List<LearningResource> getByAssessmentId(Long assessmentId) {
        return repository.findByAssessmentId(assessmentId);
    }

    // ==========================
    // DELETE
    // ==========================
    public void delete(Long id) {
        repository.deleteById(id);
    }
}