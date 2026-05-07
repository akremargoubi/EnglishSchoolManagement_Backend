import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { BackofficeAttendanceListComponent } from './attendance-list/attendance-list.component';
import { BackofficeScheduleListComponent } from './schedule-list/schedule-list.component';

@Component({
  selector: 'app-backoffice',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule,
    BackofficeAttendanceListComponent,
    BackofficeScheduleListComponent
  ],
  templateUrl: './backoffice.html',
  styleUrls: ['./backoffice.css']
})
export class Backoffice {
  activeSection = 'dashboard';

  stats = {
    totalStudents: 420,
    activeClasses: 18,
    attendanceRate: 94,
    totalTeachers: 24,
    weeklyClasses: 45,
    pendingAbsences: 3
  };

  setActiveSection(section: string) {
    this.activeSection = section;
  }
}