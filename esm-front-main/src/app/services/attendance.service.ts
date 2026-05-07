import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Attendance {
  attended?: number;
  date: string;
  status: 'PRESENT' | 'ABSENT' | 'LATE';
  studentId?: string;
}

@Injectable({ providedIn: 'root' })
export class AttendanceService {
  // MUST BE EXACTLY THIS - NO http://localhost:4200
  private apiUrl = '/api/attendances';

  constructor(private http: HttpClient) {
    console.log('API URL:', this.apiUrl); // Should log: /api/attendances
  }

  getAll(): Observable<Attendance[]> {
    console.log('Calling:', this.apiUrl); // Debug
    return this.http.get<Attendance[]>(this.apiUrl);
  }

  getById(id: number): Observable<Attendance> {
    return this.http.get<Attendance>(`${this.apiUrl}/${id}`);
  }

  create(attendance: Attendance): Observable<Attendance> {
    return this.http.post<Attendance>(this.apiUrl, attendance);
  }

  update(id: number, attendance: Attendance): Observable<Attendance> {
    return this.http.put<Attendance>(`${this.apiUrl}/${id}`, attendance);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}