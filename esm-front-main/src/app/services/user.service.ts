import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface User {
  id: string;
  uuid: string;
  firstName: string;
  lastName: string;
  email: string;
  cin?: string;
  studentClass?: { id: number; name: string; level: string };
}

export interface Class {
  id: number;
  name: string;
  level: string;
  specialty: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  // Your friend's auth service URL
  private apiUrl = 'http://localhost:1999/api';

  constructor(private http: HttpClient) {}

  getAllClasses(): Observable<Class[]> {
    return this.http.get<Class[]>(`${this.apiUrl}/classes`);
  }

  getStudentsByClass(classId: number): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/users/class/${classId}`);
  }

  getUserById(id: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/users/${id}`);
  }
}