package com.englishschool.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "attendances")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attended;

    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;

    // Constructors
    public Attendance() {}

    public Attendance(LocalDate date, AttendanceStatus status) {
        this.date = date;
        this.status = status;
    }

    // Getters and Setters
    public Long getAttended() { return attended; }
    public void setAttended(Long attended) { this.attended = attended; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
}
