import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

interface ViolationDto {
  id: string;
  referenceNumber: string;
  violationType: string;
  occurredAt?: string;
  status: string;
}

interface AppealDto {
  id: string;
  appealNumber: string;
  reason: string;
  submittedAt?: string;
  status: string;
}

interface ReportDto {
  id: string;
  reportType: string;
  status: string;
}

@Component({
  selector: 'app-officer-overview',
  standalone: false,
  templateUrl: './officer-overview.component.html',
  styleUrls: ['./officer-overview.component.css']
})
export class OfficerOverviewComponent implements OnInit {
  pendingViolations: ViolationDto[] = [];
  violationsLoading = false;
  violationsError = '';

  pendingAppeals: AppealDto[] = [];
  appealsLoading = false;
  appealsError = '';

  myReports: ReportDto[] = [];
  reportsLoading = false;
  reportsError = '';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadPendingViolations();
    this.loadPendingAppeals();
    this.loadMyReports();
  }

  get readyReports(): ReportDto[] {
    return this.myReports.filter((r) => r.status === 'GENERATED' || r.status === 'DONE');
  }

  // The backend doc describes a "pending" route on ViolationController and
  // AppealController without pinning down the exact response shape, so this
  // defensively accepts either a plain list or a PagedResult wrapper.
  private extractList(data: any): any[] {
    if (Array.isArray(data)) return data;
    if (Array.isArray(data?.resultList)) return data.resultList;
    return [];
  }

  loadPendingViolations(): void {
    this.violationsLoading = true;
    this.violationsError = '';

    this.http.get<any>('/api/violations/pending').subscribe({
      next: (response) => {
        this.pendingViolations = this.extractList(response?.data);
        this.violationsLoading = false;
      },
      error: () => {
        this.violationsError = 'Unable to load pending violations.';
        this.violationsLoading = false;
      }
    });
  }

  loadPendingAppeals(): void {
    this.appealsLoading = true;
    this.appealsError = '';

    this.http.get<any>('/api/appeals/pending').subscribe({
      next: (response) => {
        this.pendingAppeals = this.extractList(response?.data);
        this.appealsLoading = false;
      },
      error: () => {
        this.appealsError = 'Unable to load pending appeals.';
        this.appealsLoading = false;
      }
    });
  }

  loadMyReports(): void {
    this.reportsLoading = true;
    this.reportsError = '';

    this.http.get<any>('/api/reports/my').subscribe({
      next: (response) => {
        this.myReports = this.extractList(response?.data);
        this.reportsLoading = false;
      },
      error: () => {
        this.reportsError = 'Unable to load your reports.';
        this.reportsLoading = false;
      }
    });
  }
}
