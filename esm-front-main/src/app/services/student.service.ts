import { Injectable } from '@angular/core';

export interface Student {
  id: number;
  name: string;
  email: string;
  level: string; // Beginner, Intermediate, Advanced
}

@Injectable({ providedIn: 'root' })
export class StudentService {
  
  // Mock student data - 20 students with English school appropriate names
  private students: Student[] = [
    { id: 1, name: 'Emma Watson', email: 'emma.w@email.com', level: 'Advanced' },
    { id: 2, name: 'James Smith', email: 'james.s@email.com', level: 'Intermediate' },
    { id: 3, name: 'Maria Garcia', email: 'maria.g@email.com', level: 'Beginner' },
    { id: 4, name: 'John Johnson', email: 'john.j@email.com', level: 'Intermediate' },
    { id: 5, name: 'Patricia Brown', email: 'patricia.b@email.com', level: 'Advanced' },
    { id: 6, name: 'Michael Lee', email: 'michael.l@email.com', level: 'Beginner' },
    { id: 7, name: 'Sarah Wilson', email: 'sarah.w@email.com', level: 'Intermediate' },
    { id: 8, name: 'David Martinez', email: 'david.m@email.com', level: 'Advanced' },
    { id: 9, name: 'Lisa Anderson', email: 'lisa.a@email.com', level: 'Beginner' },
    { id: 10, name: 'Robert Taylor', email: 'robert.t@email.com', level: 'Intermediate' },
    { id: 11, name: 'Jennifer Thomas', email: 'jennifer.t@email.com', level: 'Advanced' },
    { id: 12, name: 'William Jackson', email: 'william.j@email.com', level: 'Beginner' },
    { id: 13, name: 'Elizabeth White', email: 'elizabeth.w@email.com', level: 'Intermediate' },
    { id: 14, name: 'Charles Harris', email: 'charles.h@email.com', level: 'Advanced' },
    { id: 15, name: 'Margaret Martin', email: 'margaret.m@email.com', level: 'Beginner' },
    { id: 16, name: 'Joseph Thompson', email: 'joseph.t@email.com', level: 'Intermediate' },
    { id: 17, name: 'Sandra Robinson', email: 'sandra.r@email.com', level: 'Advanced' },
    { id: 18, name: 'Thomas Clark', email: 'thomas.c@email.com', level: 'Beginner' },
    { id: 19, name: 'Nancy Rodriguez', email: 'nancy.r@email.com', level: 'Intermediate' },
    { id: 20, name: 'Daniel Lewis', email: 'daniel.l@email.com', level: 'Advanced' }
  ];

  getAllStudents(): Student[] {
    return this.students;
  }

  getStudentById(id: number): Student | undefined {
    // Since attendance IDs might not match student IDs, we'll map modulo
    // This ensures every attendance ID gets a student
    const studentIndex = (id - 1) % this.students.length;
    return this.students[studentIndex];
  }

  getStudentName(id: number): string {
    const student = this.getStudentById(id);
    return student ? student.name : `Student #${id}`;
  }

  getStudentLevel(id: number): string {
    const student = this.getStudentById(id);
    return student ? student.level : 'Beginner';
  }
}