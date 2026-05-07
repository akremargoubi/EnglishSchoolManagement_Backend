import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ScheduleService, Schedule } from '../../services/schedule.service';

@Component({
  selector: 'app-schedule-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './schedule-form.component.html'
})
export class ScheduleFormComponent implements OnInit {
  schedule: Schedule = {
    dayOfWeek: 'Monday',
    startTime: '09:00',
    endTime: '10:30',
    room: ''
  };
  isEdit = false;
  loading = false;
  submitted = false;

  daysOfWeek = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  
  // Validation errors
  errors: { [key: string]: string } = {};

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private scheduleService: ScheduleService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.loadSchedule(+id);
    }
  }

  loadSchedule(id: number): void {
    this.loading = true;
    this.scheduleService.getById(id).subscribe({
      next: (data) => {
        this.schedule = data;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading schedule:', error);
        this.loading = false;
      }
    });
  }

  // Validation methods
  validateForm(): boolean {
    this.errors = {};
    let isValid = true;

    // Room validation
    if (!this.schedule.room || this.schedule.room.trim() === '') {
      this.errors['room'] = 'Room number is required';
      isValid = false;
    } else if (this.schedule.room.length < 2) {
      this.errors['room'] = 'Room number must be at least 2 characters';
      isValid = false;
    }

    // Time validation
    if (this.schedule.startTime && this.schedule.endTime) {
      const start = this.convertTimeToMinutes(this.schedule.startTime);
      const end = this.convertTimeToMinutes(this.schedule.endTime);
      
      if (start >= end) {
        this.errors['time'] = 'End time must be after start time';
        isValid = false;
      }
      
      // Check minimum duration (30 minutes)
      if (end - start < 30) {
        this.errors['time'] = 'Schedule must be at least 30 minutes long';
        isValid = false;
      }
      
      // Check maximum duration (4 hours)
      if (end - start > 240) {
        this.errors['time'] = 'Schedule cannot exceed 4 hours';
        isValid = false;
      }
    }

    // Day validation
    if (!this.schedule.dayOfWeek) {
      this.errors['dayOfWeek'] = 'Please select a day';
      isValid = false;
    }

    return isValid;
  }

  convertTimeToMinutes(time: string): number {
    const [hours, minutes] = time.split(':').map(Number);
    return hours * 60 + minutes;
  }

  onSubmit(form: NgForm): void {
    this.submitted = true;
    
    if (!this.validateForm()) {
      // Scroll to top to show errors
      window.scrollTo(0, 0);
      return;
    }
    
    this.loading = true;
    
    if (this.isEdit) {
      this.scheduleService.update(this.schedule.scheduled!, this.schedule).subscribe({
        next: () => {
          this.router.navigate(['/schedule']);
        },
        error: (error) => {
          console.error('Error updating:', error);
          this.errors['general'] = 'Failed to save schedule. Please try again.';
          this.loading = false;
        }
      });
    } else {
      this.scheduleService.create(this.schedule).subscribe({
        next: () => {
          this.router.navigate(['/schedule']);
        },
        error: (error) => {
          console.error('Error creating:', error);
          this.errors['general'] = 'Failed to create schedule. Please try again.';
          this.loading = false;
        }
      });
    }
  }

  // Helper to check if field is invalid
  isFieldInvalid(fieldName: string): boolean {
    return this.submitted && !!this.errors[fieldName];
  }
}