import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../core/auth.service';
import { Router } from '@angular/router';

interface StatisticsDto {
  totalViolations: number;
  totalFinesIssued: number;
  collectionRate: number;
  activeCameras: number;
  appealsApproved: number;
  appealsSubmitted: number;
}

interface ReportDto {
  reportType: string;
  status: string;
  ready: boolean;
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  standalone: false,
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  role: string | null = null;
  stats: StatisticsDto | null = null;
  reports: ReportDto[] = [];
  reportLoading = false;
  loading = false;
  errorMessage = '';

  constructor(private http: HttpClient, private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.role = this.authService.getRole();
    if (!this.role) {
      this.router.navigate(['/login']);
      return;
    }
    this.loadStatistics();
    this.loadReports();
  }

  get canManageSystem(): boolean {
    return this.role === 'ADMIN';
  }

  get canRecordViolations(): boolean {
    return this.role === 'OFFICER' || this.role === 'ADMIN';
  }

  get canViewReports(): boolean {
    return this.role === 'OFFICER' || this.role === 'ADMIN';
  }

  loadStatistics(): void {
    this.loading = true;
    this.http.get<any>('/api/analytics/statistics?periodType=MONTHLY&from=2026-01-01&to=2026-12-31').subscribe({
      next: (response) => {
        this.stats = response?.data ?? null;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Unable to load analytics snapshot.';
        this.loading = false;
      }
    });
  }

  loadReports(): void {
    this.reportLoading = true;

    this.http.get<any>('/api/reports/my').subscribe({
      next: (response) => {
        this.reports = Array.isArray(response?.data) ? response.data : [];
        this.reportLoading = false;
      },
      error: () => {
        this.reportLoading = false;
        this.errorMessage = 'Unable to load reports.';
      }
    });
  }

  generateReport(): void {
    this.http.post<any>('/api/reports', {
      reportType: 'MONTHLY_FINES',
      format: 'PDF',
      periodStart: '2026-01-01',
      periodEnd: '2026-01-31'
    }).subscribe({
      next: () => this.loadReports(),
      error: () => this.errorMessage = 'Could not generate report.'
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
