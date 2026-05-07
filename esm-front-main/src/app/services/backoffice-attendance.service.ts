import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface BackofficeAttendance {
  id?: number;
  studentId: number;
  studentName: string;
  date: string;
  status: 'PRESENT' | 'ABSENT' | 'LATE';
  classLevel: 'Beginner' | 'Intermediate' | 'Advanced';
  teacher: string;
  notes?: string;
}

@Injectable({ providedIn: 'root' })
export class BackofficeAttendanceService {
  private apiUrl = 'http://localhost:8080/api/attendances';

  private mockStudents = [
    { id: 1, name: 'Emma Watson', level: 'Advanced' as const },
    { id: 2, name: 'James Smith', level: 'Intermediate' as const },
    { id: 3, name: 'Maria Garcia', level: 'Beginner' as const },
    { id: 4, name: 'John Johnson', level: 'Intermediate' as const },
    { id: 5, name: 'Patricia Brown', level: 'Advanced' as const },
  ];

  private mockTeachers = ['Sarah Johnson', 'Michael Lee', 'Emily Clark', 'David Brown'];

  constructor(private http: HttpClient) {}

  getAll(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  transformToBackoffice(data: any[]): BackofficeAttendance[] {
    return data.map((item, index) => ({
      id: item.attended || item.id,
      studentId: index + 1,
      studentName: this.mockStudents[index % this.mockStudents.length].name,
      date: item.date,
      status: item.status,
      classLevel: this.mockStudents[index % this.mockStudents.length].level,
      teacher: this.mockTeachers[index % this.mockTeachers.length],
      notes: ''
    }));
  }

  getById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  create(attendance: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, attendance);
  }

  update(id: number, attendance: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, attendance);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}