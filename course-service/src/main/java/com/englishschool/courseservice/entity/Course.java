package com.englishschool.courseservice.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long courseId;

    private String name;
    private String level;
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id")
    private Instructor instructor;

    @jakarta.persistence.OneToMany(mappedBy = "course", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @jakarta.persistence.OrderBy("orderIndex ASC")
    private List<Module> modules = new ArrayList<>();

    @Column(name = "price", precision = 10, scale = 2)
    private java.math.BigDecimal price;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "is_published")
    private Boolean isPublished = false;

    @Column(name = "rating_avg", precision = 3, scale = 2)
    private java.math.BigDecimal ratingAvg;

    @Column(name = "rating_count")
    private Integer ratingCount = 0;

    public Course() {
    }

    public Course(Long courseId, String name, String level, String description) {
        this.courseId = courseId;
        this.name = name;
        this.level = level;
        this.description = description;
    }

    // Getters & Setters

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Instructor getInstructor() { return instructor; }
    public void setInstructor(Instructor instructor) { this.instructor = instructor; }
    public List<Module> getModules() { return modules; }
    public void setModules(List<Module> modules) { this.modules = modules != null ? modules : new ArrayList<>(); }
    public java.math.BigDecimal getPrice() { return price; }
    public void setPrice(java.math.BigDecimal price) { this.price = price; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public Boolean getIsPublished() { return isPublished; }
    public void setIsPublished(Boolean isPublished) { this.isPublished = isPublished; }
    public java.math.BigDecimal getRatingAvg() { return ratingAvg; }
    public void setRatingAvg(java.math.BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; }
    public Integer getRatingCount() { return ratingCount; }
    public void setRatingCount(Integer ratingCount) { this.ratingCount = ratingCount; }
}
