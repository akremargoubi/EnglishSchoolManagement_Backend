import { Routes } from '@angular/router';
import { Content } from './content/content';
import { Signin } from './pages/signin/signin';
import { Signup } from './pages/signup/signup';
import { Backoffice } from './pages/backoffice/backoffice';
import { AttendanceListComponent } from './attendance/attendance-list/attendance-list.component';
import { AttendanceFormComponent } from './attendance/attendance-form/attendance-form.component';
import { ScheduleListComponent } from './schedule/schedule-list/schedule-list.component';
import { ScheduleFormComponent } from './schedule/schedule-form/schedule-form.component';
import { BackofficeAttendanceListComponent } from './pages/backoffice/attendance-list/attendance-list.component';
import { BackofficeAttendanceFormComponent } from './pages/backoffice/attendance-form/attendance-form.component';
import { BackofficeScheduleListComponent } from './pages/backoffice/schedule-list/schedule-list.component';
import { BackofficeScheduleFormComponent } from './pages/backoffice/schedule-form/schedule-form.component';

export const routes: Routes = [
  { path: '', component: Content },
  { path: 'signin', component: Signin },
  { path: 'signup', component: Signup },
  { path: 'forgot-password', component: Signin },
  { path: 'support', component: Signin },
  { path: 'backoffice', component: Backoffice },
  
  // Attendance CRUD routes
  { path: 'attendance', component: AttendanceListComponent },
  { path: 'attendance/new', component: AttendanceFormComponent },
  { path: 'attendance/edit/:id', component: AttendanceFormComponent },
  
  // Schedule CRUD routes
  { path: 'schedule', component: ScheduleListComponent },
  { path: 'schedule/new', component: ScheduleFormComponent },
  { path: 'schedule/edit/:id', component: ScheduleFormComponent },

  // Backoffice CRUD routes
  { path: 'backoffice/attendance', component: BackofficeAttendanceListComponent },
  { path: 'backoffice/attendance/new', component: BackofficeAttendanceFormComponent },
  { path: 'backoffice/attendance/edit/:id', component: BackofficeAttendanceFormComponent },
  { path: 'backoffice/schedule', component: BackofficeScheduleListComponent },
  { path: 'backoffice/schedule/new', component: BackofficeScheduleFormComponent },
  { path: 'backoffice/schedule/edit/:id', component: BackofficeScheduleFormComponent },

  { path: '**', redirectTo: '' }
];