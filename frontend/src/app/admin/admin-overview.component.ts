import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

interface StatisticsDto {
  totalViolations: number;
  autoDetected: number;
  manuallyRecorded: number;
  totalFinesIssued: number;
  totalFinesAmount: number;
  totalCollected: number;
  totalOverdue: number;
  appealsSubmitted: number;
  appealsApproved: number;
  activeCameras: number;
}

interface ReportDto {
  id: string;
  reportType: string;
  format: string;
  status: string;
  requestedBy?: string;
  completedAt?: string;
}

@Component({
  selector: 'app-admin-overview',
  standalone: false,
  templateUrl: './admin-overview.component.html',
  styleUrls: ['./admin-overview.component.css']
})
export class AdminOverviewComponent implements OnInit {
  stats: StatisticsDto | null = null;
  statsLoading = false;
  statsError = '';

  recentReports: ReportDto[] = [];
  reportsLoading = false;
  reportsError = '';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadStatistics();
    this.loadRecentReports();
  }

  loadStatistics(): void {
    this.statsLoading = true;
    this.statsError = '';

    this.http
      .get<any>('/api/analytics/statistics?periodType=MONTHLY&from=2026-01-01&to=2026-12-31')
      .subscribe({
        next: (response) => {
          this.stats = response?.data ?? null;
          this.statsLoading = false;
        },
        error: () => {
          this.statsError = 'Unable to load the statistics snapshot.';
          this.statsLoading = false;
        }
      });
  }

  loadRecentReports(): void {
    this.reportsLoading = true;
    this.reportsError = '';

    this.http.get<any>('/api/reports?limit=5&order=createdDate&orderDirection=DESC').subscribe({
      next: (response) => {
        this.recentReports = Array.isArray(response?.data?.resultList)
          ? response.data.resultList
          : [];
        this.reportsLoading = false;
      },
      error: () => {
        this.reportsError = 'Unable to load recent reports.';
        this.reportsLoading = false;
      }
    });
  }
}
