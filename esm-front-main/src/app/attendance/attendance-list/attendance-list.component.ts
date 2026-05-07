import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AttendanceService, Attendance } from '../../services/attendance.service';
import { AttendanceAnalyticsComponent } from '../attendance-analytics/attendance-analytics.component';

@Component({
  selector: 'app-attendance-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, AttendanceAnalyticsComponent],
  templateUrl: './attendance-list.component.html'
})
export class AttendanceListComponent implements OnInit {
  attendances: Attendance[] = [];
  filteredAttendances: Attendance[] = [];
  loading = false;
  
  // These properties match your HTML template
  selectedClass = 'all';
  classes: any[] = [];  // Will be populated from API
  
  // Filters
  searchTerm = '';
  selectedLevel = 'all';
  selectedStatus = 'all';
  selectedDate = '';

  levels = ['Beginner', 'Intermediate', 'Advanced'];
  statuses = ['PRESENT', 'ABSENT', 'LATE'];

  totalPresent = 0;
  totalAbsent = 0;
  totalLate = 0;
  activeTab: 'list' | 'analytics' = 'list';

  constructor(private attendanceService: AttendanceService) {}

  ngOnInit(): void {
    
    this.loadAttendances();
    this.loadClasses();
  }
  

  loadClasses(): void {
    // For now, use empty array - will be populated when User API is ready
    this.classes = [
      { id: 1, name: 'DS3' },
      { id: 2, name: 'TWIN1' },
      { id: 3, name: 'GL3' }
    ];
  }

  loadAttendances(): void {
  this.loading = true;
  this.attendanceService.getAll().subscribe({
    next: (data) => {
      console.log('Data received:', data);
      this.attendances = data;
      this.filteredAttendances = data;
      this.applyFilters();
      this.calculateStats();
      this.loading = false;
    },
    error: (error) => {
      console.error('Error loading attendances:', error);
      this.loading = false;
    }
  });
}

  applyFilters(): void {
    this.filteredAttendances = this.attendances.filter(a => {
      const matchesStatus = this.selectedStatus === 'all' || a.status === this.selectedStatus;
      const matchesDate = !this.selectedDate || a.date === this.selectedDate;
      const matchesClass = this.selectedClass === 'all' || true;
      return matchesStatus && matchesDate && matchesClass;
    });
  }

  calculateStats(): void {
    this.totalPresent = this.attendances.filter(a => a.status === 'PRESENT').length;
    this.totalAbsent = this.attendances.filter(a => a.status === 'ABSENT').length;
    this.totalLate = this.attendances.filter(a => a.status === 'LATE').length;
  }

  deleteAttendance(id: number): void {
    if (confirm('Are you sure you want to delete this attendance record?')) {
      this.attendanceService.delete(id).subscribe({
        next: () => {
          this.loadAttendances();
        },
        error: (error) => console.error('Error deleting:', error)
      });
    }
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedClass = 'all';
    this.selectedLevel = 'all';
    this.selectedStatus = 'all';
    this.selectedDate = '';
    this.filteredAttendances = this.attendances;
  }

  // These methods are required by your HTML template
  getStudentName(studentId: string): string {
    return `Student ${studentId?.substring(0, 8) || 'Unknown'}`;
  }

  getStudentClass(studentId: string): string {
    return 'N/A';
  }

  getStatusColor(status: string): string {
    const colors: Record<string, string> = {
      'PRESENT': 'bg-green-100 text-green-800',
      'ABSENT': 'bg-red-100 text-red-800',
      'LATE': 'bg-yellow-100 text-yellow-800'
    };
    return colors[status] || 'bg-gray-100 text-gray-800';
  }

  getStatusIcon(status: string): string {
    switch(status) {
      case 'PRESENT': return '✅';
      case 'ABSENT': return '❌';
      case 'LATE': return '⏰';
      default: return '📝';
    }
  }
}