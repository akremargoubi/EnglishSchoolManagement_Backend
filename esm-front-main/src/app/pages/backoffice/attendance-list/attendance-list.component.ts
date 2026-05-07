import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BackofficeAttendanceService, BackofficeAttendance } from '../../../services/backoffice-attendance.service';

@Component({
  selector: 'app-backoffice-attendance-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './attendance-list.component.html'
})
export class BackofficeAttendanceListComponent implements OnInit {
  attendances: BackofficeAttendance[] = [];
  filteredAttendances: BackofficeAttendance[] = [];
  loading = false;
  
  searchTerm = '';
  selectedLevel = 'all';
  selectedStatus = 'all';
  selectedDate = '';

  levels = ['Beginner', 'Intermediate', 'Advanced'];
  statuses = ['PRESENT', 'ABSENT', 'LATE'];

  totalPresent = 0;
  totalAbsent = 0;
  totalLate = 0;

  constructor(private attendanceService: BackofficeAttendanceService) {}

  ngOnInit(): void {
    this.loadAttendances();
  }

  loadAttendances(): void {
    this.loading = true;
    this.attendanceService.getAll().subscribe({
      next: (data: any[]) => {
        this.attendances = this.attendanceService.transformToBackoffice(data);
        this.applyFilters();
        this.calculateStats();
        this.loading = false;
      },
      error: (error: any) => {
        console.error('Error loading attendances:', error);
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteredAttendances = this.attendances.filter(a => {
      const matchesSearch = !this.searchTerm || 
        a.studentName.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchesLevel = this.selectedLevel === 'all' || a.classLevel === this.selectedLevel;
      const matchesStatus = this.selectedStatus === 'all' || a.status === this.selectedStatus;
      const matchesDate = !this.selectedDate || a.date === this.selectedDate;
      return matchesSearch && matchesLevel && matchesStatus && matchesDate;
    });
  }

  calculateStats(): void {
    this.totalPresent = this.attendances.filter(a => a.status === 'PRESENT').length;
    this.totalAbsent = this.attendances.filter(a => a.status === 'ABSENT').length;
    this.totalLate = this.attendances.filter(a => a.status === 'LATE').length;
  }

  deleteAttendance(id: number | undefined): void {
    if (!id) return;
    if (confirm('Delete this attendance record?')) {
      this.attendanceService.delete(id).subscribe({
        next: () => this.loadAttendances(),
        error: (error: any) => console.error('Delete error:', error)
      });
    }
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedLevel = 'all';
    this.selectedStatus = 'all';
    this.selectedDate = '';
    this.filteredAttendances = this.attendances;
  }

  getStatusClass(status: string): string {
    const classes: {[key: string]: string} = {
      'PRESENT': 'bg-green-100 text-green-800',
      'ABSENT': 'bg-red-100 text-red-800',
      'LATE': 'bg-yellow-100 text-yellow-800'
    };
    return classes[status] || 'bg-gray-100 text-gray-800';
  }
}