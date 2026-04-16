package com.esprit.esmauthms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "classes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;           // ex: "TWIN1", "DS3"

    private String level;          // ex: "3ème année"

    private String specialty;      // ex: "Informatique"

    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Relation : une classe a plusieurs étudiants
    @OneToMany(mappedBy = "studentClass", fetch = FetchType.LAZY)
    private List<User> students;
}