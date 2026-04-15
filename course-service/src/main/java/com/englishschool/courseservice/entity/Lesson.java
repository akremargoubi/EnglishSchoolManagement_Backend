package com.englishschool.courseservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {

    public enum ContentType { VIDEO, PDF, QUIZ, TEXT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 20)
    private ContentType contentType;

    @Column(name = "content_url", length = 500)
    private String contentUrl;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "quiz_content_json", columnDefinition = "TEXT")
    private String quizContentJson;

    @Column(name = "duration_minutes")
    private Integer durationMinutes = 0;

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    @PrePersist
    protected void onCreate() {
        if (orderIndex == null) orderIndex = 0;
        if (durationMinutes == null) durationMinutes = 0;
    }
}
