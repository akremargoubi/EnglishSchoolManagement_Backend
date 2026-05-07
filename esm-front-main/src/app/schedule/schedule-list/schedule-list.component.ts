import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ScheduleService, Schedule } from '../../services/schedule.service';
import { WeatherWidgetComponent } from '../weather-widget/weather-widget.component';

@Component({
  selector: 'app-schedule-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, WeatherWidgetComponent],
  templateUrl: './schedule-list.component.html'
})
export class ScheduleListComponent implements OnInit {
  schedules: Schedule[] = [];
  filteredSchedules: Schedule[] = [];
  loading = false;
  error: string | null = null;
  
  selectedDay: string = 'all';
  searchRoom: string = '';
  
  days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  timeSlots: string[] = [];

  get totalSchedules(): number {
    return this.schedules.length;
  }

  get weekdayClasses(): number {
    return this.schedules.filter(s => s.dayOfWeek !== 'Saturday' && s.dayOfWeek !== 'Sunday').length;
  }

  get weekendClasses(): number {
    return this.schedules.filter(s => s.dayOfWeek === 'Saturday' || s.dayOfWeek === 'Sunday').length;
  }

  get activeRooms(): number {
    return new Set(this.schedules.map(s => s.room).filter(room => room)).size;
  }

  constructor(private scheduleService: ScheduleService) {
    this.generateTimeSlots();
  }

  generateTimeSlots() {
    for (let hour = 8; hour < 18; hour++) {
      this.timeSlots.push(`${hour.toString().padStart(2, '0')}:00`);
      this.timeSlots.push(`${hour.toString().padStart(2, '0')}:30`);
    }
  }

  ngOnInit(): void {
    this.loadSchedules();
  }

  loadSchedules(): void {
    this.loading = true;
    this.error = null;
    console.log('Loading schedules...');
    
    this.scheduleService.getAll().subscribe({
      next: (data) => {
        console.log('Data received:', data);
        this.schedules = data;
        this.filteredSchedules = data;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading schedules:', error);
        this.error = 'Failed to load schedules. Please try again.';
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteredSchedules = this.schedules.filter(s => {
      const matchesDay = this.selectedDay === 'all' || s.dayOfWeek === this.selectedDay;
      const matchesRoom = !this.searchRoom || (s.room && s.room.toLowerCase().includes(this.searchRoom.toLowerCase()));
      return matchesDay && matchesRoom;
    });
  }

  resetFilters(): void {
    this.selectedDay = 'all';
    this.searchRoom = '';
    this.filteredSchedules = this.schedules;
  }

  deleteSchedule(id: number): void {
    if (confirm('Are you sure you want to delete this schedule?')) {
      this.scheduleService.delete(id).subscribe({
        next: () => {
          this.loadSchedules();
        },
        error: (error) => console.error('Error deleting:', error)
      });
    }
  }

  getDayColor(day: string | null): string {
    if (!day) return 'bg-gray-100 text-gray-800';
    const colors: Record<string, string> = {
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

  getSchedulesForDay(day: string): Schedule[] {
    return this.schedules.filter(s => s.dayOfWeek === day);
  }

  getFilteredSchedulesForDay(day: string): Schedule[] {
    return this.filteredSchedules.filter(s => s.dayOfWeek === day);
  }

  getTimePosition(time: string | null): string {
    if (!time) return '0px';
    const [hours, minutes] = time.split(':').map(Number);
    const totalMinutes = (hours - 8) * 60 + minutes;
    return `${(totalMinutes / 30) * 40}px`;
  }

  getTimeHeight(startTime: string | null, endTime: string | null): string {
    if (!startTime || !endTime) return '40px';
    const [startHours, startMinutes] = startTime.split(':').map(Number);
    const [endHours, endMinutes] = endTime.split(':').map(Number);
    
    const startTotal = startHours * 60 + startMinutes;
    const endTotal = endHours * 60 + endMinutes;
    const durationMinutes = endTotal - startTotal;
    
    return `${(durationMinutes / 30) * 40}px`;
  }

  formatTime(time: string | null): string {
    if (!time) return '--:--';
    return time.substring(0, 5);
  }

  // Advanced features with null safety
  getConflicts(): { schedule1: Schedule, schedule2: Schedule }[] {
    const conflicts: { schedule1: Schedule, schedule2: Schedule }[] = [];
    
    const validSchedules = this.schedules.filter(s => 
      s.dayOfWeek && s.startTime && s.endTime
    );
    
    for (let i = 0; i < validSchedules.length; i++) {
      for (let j = i + 1; j < validSchedules.length; j++) {
        const s1 = validSchedules[i];
        const s2 = validSchedules[j];
        
        if (s1.dayOfWeek !== s2.dayOfWeek) continue;
        
        const s1Start = this.convertToMinutes(s1.startTime!);
        const s1End = this.convertToMinutes(s1.endTime!);
        const s2Start = this.convertToMinutes(s2.startTime!);
        const s2End = this.convertToMinutes(s2.endTime!);
        
        if (s1Start < s2End && s2Start < s1End) {
          conflicts.push({ schedule1: s1, schedule2: s2 });
        }
      }
    }
    
    return conflicts;
  }

  convertToMinutes(time: string): number {
    if (!time) return 0;
    const [hours, minutes] = time.split(':').map(Number);
    return hours * 60 + minutes;
  }

  getMostUsedRoom(): string {
    const roomCount: { [key: string]: number } = {};
    const validSchedules = this.schedules.filter(s => s.room);
    
    validSchedules.forEach(s => {
      roomCount[s.room] = (roomCount[s.room] || 0) + 1;
    });
    
    let mostUsed = '';
    let maxCount = 0;
    
    for (const [room, count] of Object.entries(roomCount)) {
      if (count > maxCount) {
        maxCount = count;
        mostUsed = room;
      }
    }
    
    return mostUsed || 'None';
  }

  getPeakDay(): string {
    const dayCount: { [key: string]: number } = {};
    const validSchedules = this.schedules.filter(s => s.dayOfWeek);
    
    validSchedules.forEach(s => {
      dayCount[s.dayOfWeek!] = (dayCount[s.dayOfWeek!] || 0) + 1;
    });
    
    let peakDay = '';
    let maxCount = 0;
    
    for (const [day, count] of Object.entries(dayCount)) {
      if (count > maxCount) {
        maxCount = count;
        peakDay = day;
      }
    }
    
    return peakDay || 'N/A';
  }

  getNextDateForDay(day: string): string {
    const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    const today = new Date();
    const targetDay = days.indexOf(day);
    const currentDay = today.getDay();
    let daysToAdd = targetDay - currentDay;
    if (daysToAdd <= 0) daysToAdd += 7;
    const targetDate = new Date(today);
    targetDate.setDate(today.getDate() + daysToAdd);
    return targetDate.toISOString().split('T')[0];
  }
}