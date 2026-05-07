import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private attendanceApi = 'http://localhost:8080/api/attendances';
  private scheduleApi = 'http://localhost:8080/api/schedules';

  constructor(private http: HttpClient) {}

  // Attendance endpoints
  getAttendances(): Observable<any> {
    return this.http.get(this.attendanceApi);
  }

  // Schedule endpoints
  getSchedules(): Observable<any> {
    return this.http.get(this.scheduleApi);
  }
}