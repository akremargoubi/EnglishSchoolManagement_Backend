import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface BackofficeSchedule {
  id?: number;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  room: string;
  courseName: string;
  teacher: string;
  level: 'Beginner' | 'Intermediate' | 'Advanced';
  maxStudents: number;
  enrolledStudents: number;
}

@Injectable({ providedIn: 'root' })
export class BackofficeScheduleService {
  private apiUrl = 'http://localhost:8080/api/schedules';

  private mockCourses = [
    { name: 'Beginner English - Grammar Basics', level: 'Beginner' as const },
    { name: 'Intermediate Conversation', level: 'Intermediate' as const },
    { name: 'Advanced Business English', level: 'Advanced' as const },
    { name: 'IELTS Preparation', level: 'Advanced' as const },
    { name: 'English for Tourism', level: 'Intermediate' as const },
  ];

  private mockTeachers = ['Sarah Johnson', 'Michael Lee', 'Emily Clark', 'David Brown'];

  constructor(private http: HttpClient) {}

  getAll(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  transformToBackoffice(data: any[]): BackofficeSchedule[] {
    return data.map((item, index) => ({
      id: item.scheduled || item.id,
      dayOfWeek: item.dayOfWeek,
      startTime: item.startTime,
      endTime: item.endTime,
      room: item.room,
      courseName: this.mockCourses[index % this.mockCourses.length].name,
      teacher: this.mockTeachers[index % this.mockTeachers.length],
      level: this.mockCourses[index % this.mockCourses.length].level,
      maxStudents: 20,
      enrolledStudents: Math.floor(Math.random() * 15) + 5
    }));
  }

  getById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  create(schedule: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, schedule);
  }

  update(id: number, schedule: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, schedule);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}