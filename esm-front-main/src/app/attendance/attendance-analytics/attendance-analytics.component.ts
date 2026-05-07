import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AttendanceService, Attendance } from '../../services/attendance.service';
import { StudentService } from '../../services/student.service';

interface StudentStats {
  studentId: number;
  studentName: string;
  level: string;
  totalClasses: number;
  presentCount: number;
  lateCount: number;
  absentCount: number;
  attendanceRate: number;
  trend: 'up' | 'down' | 'stable';
  riskLevel: 'low' | 'medium' | 'high';
}

@Component({
  selector: 'app-attendance-analytics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './attendance-analytics.component.html'
})
export class AttendanceAnalyticsComponent implements OnInit {
  attendances: Attendance[] = [];
  studentStats: StudentStats[] = [];
  loading = false;
  
  globalRate = 0;
  atRiskStudents = 0;
  improvingStudents = 0;

  constructor(
    private attendanceService: AttendanceService,
    public studentService: StudentService
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.attendanceService.getAll().subscribe({
      next: (data) => {
        this.attendances = data;
        this.calculateStudentStats();
        this.calculateGlobalStats();
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
      }
    });
  }

  calculateStudentStats(): void {
    const studentMap = new Map<number, Attendance[]>();
    
    this.attendances.forEach(att => {
      const studentId = att.attended!;
      if (!studentMap.has(studentId)) {
        studentMap.set(studentId, []);
      }
      studentMap.get(studentId)!.push(att);
    });

    this.studentStats = Array.from(studentMap.entries()).map(([id, records]) => {
      const total = records.length;
      const present = records.filter(r => r.status === 'PRESENT').length;
      const late = records.filter(r => r.status === 'LATE').length;
      const absent = records.filter(r => r.status === 'ABSENT').length;
      const rate = total > 0 ? (present + late) / total * 100 : 0;
      
      const sorted = [...records].sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
      const midPoint = Math.floor(sorted.length / 2);
      const recent = sorted.slice(midPoint);
      const earlier = sorted.slice(0, midPoint);
      
      const recentRate = recent.length > 0 ? (recent.filter(r => r.status !== 'ABSENT').length) / recent.length * 100 : 0;
      const earlierRate = earlier.length > 0 ? (earlier.filter(r => r.status !== 'ABSENT').length) / earlier.length * 100 : 0;
      
      let trend: 'up' | 'down' | 'stable' = 'stable';
      if (recentRate > earlierRate + 5) trend = 'up';
      else if (recentRate < earlierRate - 5) trend = 'down';
      
      let riskLevel: 'low' | 'medium' | 'high' = 'low';
      if (rate < 60) riskLevel = 'high';
      else if (rate < 80) riskLevel = 'medium';
      
      return {
        studentId: id,
        studentName: this.studentService.getStudentName(id),
        level: this.studentService.getStudentLevel(id),
        totalClasses: total,
        presentCount: present,
        lateCount: late,
        absentCount: absent,
        attendanceRate: Math.round(rate),
        trend,
        riskLevel
      };
    });
    
    this.studentStats.sort((a, b) => a.attendanceRate - b.attendanceRate);
  }

  calculateGlobalStats(): void {
    const total = this.attendances.length;
    const present = this.attendances.filter(a => a.status !== 'ABSENT').length;
    this.globalRate = total > 0 ? Math.round(present / total * 100) : 0;
    this.atRiskStudents = this.studentStats.filter(s => s.riskLevel === 'high').length;
    this.improvingStudents = this.studentStats.filter(s => s.trend === 'up').length;
  }

  getRiskColor(risk: string): string {
    switch(risk) {
      case 'high': return 'bg-red-100 text-red-800 border-red-200';
      case 'medium': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      default: return 'bg-green-100 text-green-800 border-green-200';
    }
  }

  getTrendIcon(trend: string): string {
    switch(trend) {
      case 'up': return '📈';
      case 'down': return '📉';
      default: return '➡️';
    }
  }

  getRiskMessage(risk: string, rate: number): string {
    if (risk === 'high') return `⚠️ Critical: ${rate}% attendance - Needs intervention`;
    if (risk === 'medium') return `⚠️ Warning: ${rate}% attendance - Monitor closely`;
    return `✅ Good: ${rate}% attendance`;
  }

  exportReport(): void {
    const csv = this.generateCSV();
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `attendance-report-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  generateCSV(): string {
    const headers = ['Student Name', 'Level', 'Total Classes', 'Present', 'Late', 'Absent', 'Attendance Rate', 'Status'];
    const rows = this.studentStats.map(s => [
      s.studentName,
      s.level,
      s.totalClasses,
      s.presentCount,
      s.lateCount,
      s.absentCount,
      `${s.attendanceRate}%`,
      s.riskLevel === 'high' ? 'At Risk' : s.riskLevel === 'medium' ? 'Monitor' : 'Good'
    ]);
    return [headers, ...rows].map(row => row.join(',')).join('\n');
  }
}