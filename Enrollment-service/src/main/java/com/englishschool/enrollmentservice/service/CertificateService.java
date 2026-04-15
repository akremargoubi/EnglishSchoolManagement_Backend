package com.englishschool.enrollmentservice.service;

import com.englishschool.enrollmentservice.client.CourseDTO;
import com.englishschool.enrollmentservice.client.CourseFeignClient;
import com.englishschool.enrollmentservice.entity.Certificate;
import com.englishschool.enrollmentservice.entity.Enrollment;
import com.englishschool.enrollmentservice.exception.ResourceNotFoundException;
import com.englishschool.enrollmentservice.repository.CertificateRepository;
import com.englishschool.enrollmentservice.repository.EnrollmentRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.awt.Color;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseFeignClient courseFeignClient;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy")
            .withZone(ZoneId.systemDefault());

    @Transactional
    public byte[] generateCertificatePdf(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", enrollmentId));
        if (!"completed".equals(enrollment.getStatus())) {
            throw new IllegalStateException("Certificate can only be generated for completed enrollments");
        }
        Certificate cert = certificateRepository.findByEnrollment_Id(enrollmentId)
                .orElseGet(() -> {
                    Certificate c = Certificate.builder()
                            .enrollment(enrollment)
                            .userId(enrollment.getUserId())
                            .courseId(enrollment.getCourseId())
                            .build();
                    return certificateRepository.save(c);
                });

        String courseName = "Course";
        try {
            CourseDTO course = courseFeignClient.getCourseById(enrollment.getCourseId());
            if (course != null && course.getName() != null) courseName = course.getName();
        } catch (Exception e) {
            log.warn("Could not fetch course for certificate: {}", e.getMessage());
        }

        return createPdf(cert, enrollment.getStudentName(), courseName);
    }

    public Optional<Certificate> getByEnrollmentId(Long enrollmentId) {
        return certificateRepository.findByEnrollment_Id(enrollmentId);
    }

    public java.util.List<Certificate> getByUserId(Long userId) {
        return certificateRepository.findByUserId(userId);
    }

    // ESM brand colors
    private static final Color VIOLET_DARK = new Color(88, 28, 135);
    private static final Color VIOLET_MID = new Color(124, 58, 237);
    private static final Color EMERALD = new Color(5, 150, 105);
    private static final Color GOLD = new Color(217, 119, 6);
    private static final Color GRAY_DARK = new Color(75, 85, 99);
    private static final Color GRAY_MUTED = new Color(107, 114, 128);

    private byte[] createPdf(Certificate cert, String studentName, String courseName) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 50, 50, 50, 50);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();

            // Draw decorative border (under content so it appears behind text)
            PdfContentByte cb = writer.getDirectContentUnder();
            float w = doc.getPageSize().getWidth();
            float h = doc.getPageSize().getHeight();
            float margin = 40;

            cb.setLineWidth(3);
            cb.setRGBColorStroke(124, 58, 237);
            cb.rectangle(margin, margin, w - 2 * margin, h - 2 * margin);
            cb.stroke();

            cb.setLineWidth(0.5f);
            cb.setRGBColorStroke(88, 28, 135);
            cb.rectangle(margin + 8, margin + 8, w - 2 * margin - 16, h - 2 * margin - 16);
            cb.stroke();

            // Decorative corner accents
            cb.setRGBColorStroke(217, 119, 6);
            cb.setLineWidth(1.5f);
            int cornerLen = 25;
            cb.moveTo(margin, margin + cornerLen);
            cb.lineTo(margin, margin);
            cb.lineTo(margin + cornerLen, margin);
            cb.stroke();
            cb.moveTo(w - margin - cornerLen, margin);
            cb.lineTo(w - margin, margin);
            cb.lineTo(w - margin, margin + cornerLen);
            cb.stroke();
            cb.moveTo(w - margin, h - margin - cornerLen);
            cb.lineTo(w - margin, h - margin);
            cb.lineTo(w - margin - cornerLen, h - margin);
            cb.stroke();
            cb.moveTo(margin + cornerLen, h - margin);
            cb.lineTo(margin, h - margin);
            cb.lineTo(margin, h - margin - cornerLen);
            cb.stroke();

            Font titleFont = new Font(Font.HELVETICA, 28, Font.BOLD, VIOLET_DARK);
            Font bodyFont = new Font(Font.HELVETICA, 14, Font.NORMAL, GRAY_DARK);
            Font nameFont = new Font(Font.HELVETICA, 22, Font.BOLD, VIOLET_MID);
            Font courseFont = new Font(Font.HELVETICA, 18, Font.BOLD, EMERALD);
            Font smallFont = new Font(Font.HELVETICA, 11, Font.NORMAL, GRAY_MUTED);

            Paragraph title = new Paragraph("CERTIFICATE OF COMPLETION", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(24);
            doc.add(title);

            Paragraph intro = new Paragraph("This is to certify that", bodyFont);
            intro.setAlignment(Element.ALIGN_CENTER);
            intro.setSpacingAfter(14);
            doc.add(intro);

            Paragraph name = new Paragraph(studentName != null ? studentName : "Student", nameFont);
            name.setAlignment(Element.ALIGN_CENTER);
            name.setSpacingAfter(12);
            doc.add(name);

            Paragraph completion = new Paragraph("has successfully completed the course", bodyFont);
            completion.setAlignment(Element.ALIGN_CENTER);
            completion.setSpacingAfter(14);
            doc.add(completion);

            Paragraph course = new Paragraph("\"" + courseName + "\"", courseFont);
            course.setAlignment(Element.ALIGN_CENTER);
            course.setSpacingAfter(24);
            doc.add(course);

            String dateStr = DATE_FORMAT.format(cert.getIssuedAt() != null ? cert.getIssuedAt() : Instant.now());
            Paragraph date = new Paragraph("Issued on " + dateStr, smallFont);
            date.setAlignment(Element.ALIGN_CENTER);
            doc.add(date);

            doc.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate certificate PDF", e);
        }
    }
}
