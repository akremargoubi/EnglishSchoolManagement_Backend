import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AttendanceService, Attendance } from '../../services/attendance.service';
import { UserService, User, Class } from '../../services/user.service';

@Component({
  selector: 'app-attendance-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './attendance-form.component.html'
})
export class AttendanceFormComponent implements OnInit {
  attendance: Attendance = {
    date: new Date().toISOString().split('T')[0],
    status: 'PRESENT',
    studentId: ''
  };
  isEdit = false;
  loading = false;

  // Dropdown data
  classes: Class[] = [];
  students: User[] = [];
  selectedClassId: number | null = null;
  selectedStudent: User | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private attendanceService: AttendanceService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.loadClasses();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.loadAttendance(+id);
    }
  }

  loadClasses(): void {
    this.userService.getAllClasses().subscribe({
      next: (data) => {
        this.classes = data;
      },
      error: (err) => console.error('Error loading classes:', err)
    });
  }

  onClassChange(): void {
    if (this.selectedClassId) {
      this.userService.getStudentsByClass(this.selectedClassId).subscribe({
        next: (data) => {
          this.students = data;
        },
        error: (err) => console.error('Error loading students:', err)
      });
    } else {
      this.students = [];
    }
  }

  onStudentChange(): void {
    if (this.selectedStudent) {
      this.attendance.studentId = this.selectedStudent.id;
    }
  }

  loadAttendance(id: number): void {
    this.loading = true;
    this.attendanceService.getById(id).subscribe({
      next: (data) => {
        this.attendance = data;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading attendance:', error);
        this.loading = false;
      }
    });
  }

  onSubmit(): void {
    if (!this.attendance.studentId) {
      alert('Please select a student');
      return;
    }

    this.loading = true;
    
    if (this.isEdit) {
      this.attendanceService.update(this.attendance.attended!, this.attendance).subscribe({
        next: () => {
          this.router.navigate(['/attendance']);
        },
        error: (error) => {
          console.error('Error updating:', error);
          this.loading = false;
        }
      });
    } else {
      this.attendanceService.create(this.attendance).subscribe({
        next: () => {
          this.router.navigate(['/attendance']);
        },
        error: (error) => {
          console.error('Error creating:', error);
          this.loading = false;
        }
      });
    }
  }

  getStudentName(student: User): string {
    return `${student.firstName || ''} ${student.lastName || ''}`.trim() || student.email;
  }
}