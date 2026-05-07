import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BackofficeScheduleService, BackofficeSchedule } from '../../../services/backoffice-schedule.service';

@Component({
  selector: 'app-backoffice-schedule-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './schedule-list.component.html',
  styleUrls: ['./schedule-list.css']
})
export class BackofficeScheduleListComponent implements OnInit {
  schedules: BackofficeSchedule[] = [];
  filteredSchedules: BackofficeSchedule[] = [];
  loading = false;

  selectedDay = 'all';
  selectedLevel = 'all';
  searchRoom = '';

  days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  levels = ['Beginner', 'Intermediate', 'Advanced'];

  // Computed properties
  get totalClasses(): number {
    return this.schedules.length;
  }

  get activeRooms(): number {
    return new Set(this.schedules.map(s => s.room)).size;
  }

  get totalCapacity(): number {
    return this.schedules.reduce((acc, s) => acc + s.maxStudents, 0);
  }

  constructor(private scheduleService: BackofficeScheduleService) {}

  ngOnInit(): void {
    this.loadSchedules();
  }

  loadSchedules(): void {
    this.loading = true;
    this.scheduleService.getAll().subscribe({
      next: (data: any[]) => {
        this.schedules = this.scheduleService.transformToBackoffice(data);
        this.filteredSchedules = this.schedules;
        this.loading = false;
      },
      error: (error: any) => {
        console.error('Error loading schedules:', error);
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteredSchedules = this.schedules.filter(s => {
      const matchesDay = this.selectedDay === 'all' || s.dayOfWeek === this.selectedDay;
      const matchesLevel = this.selectedLevel === 'all' || s.level === this.selectedLevel;
      const matchesRoom = !this.searchRoom || s.room.toLowerCase().includes(this.searchRoom.toLowerCase());
      return matchesDay && matchesLevel && matchesRoom;
    });
  }

  // NEW: Reset filters and reload data
  resetFilters(): void {
    this.selectedDay = 'all';
    this.selectedLevel = 'all';
    this.searchRoom = '';
    this.loadSchedules(); // This reloads the data from the backend
  }

  deleteSchedule(id: number | undefined): void {
    if (!id) return;
    if (confirm('Delete this schedule?')) {
      this.scheduleService.delete(id).subscribe({
        next: () => this.loadSchedules(),
        error: (error: any) => console.error('Delete error:', error)
      });
    }
  }

  getDayClass(day: string): string {
    const colors: {[key: string]: string} = {
      'Monday': 'bg-blue-100 text-blue-800',
      'Tuesday': 'bg-green-100 text-green-800',
      'Wednesday': 'bg-purple-100 text-purple-800',
      'Thursday': 'bg-yellow-100 text-yellow-800',
      'Friday': 'bg-indigo-100 text-indigo-800',
      'Saturday': 'bg-pink-100 text-pink-800',
      'Sunday': 'bg-red-100 text-red-800'
    };
    return colors[day] || 'bg-gray-100 text-gray-800';
  }
}