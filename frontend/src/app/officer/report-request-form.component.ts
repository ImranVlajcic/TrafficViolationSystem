import { Component } from '@angular/core';
import { Router } from '@angular/router';

import { ReportService } from '../services/report.service';
import { ReportRequestDto, ReportDto } from '../models/report.model';
import { ReportFormat, ReportType } from '../models/enums';

interface ReportTypeOption {
  value: ReportType;
  label: string;
}

const REPORT_TYPE_OPTIONS: ReportTypeOption[] = [
  { value: 'MONTHLY_FINES', label: 'Monthly fines' },
  { value: 'OFFICER_ACTIVITY', label: 'Officer activity' },
  { value: 'ZONE_RANKING', label: 'Zone ranking' },
  { value: 'DRIVER_HISTORY', label: 'Driver history' },
  { value: 'CAMERA_UPTIME', label: 'Camera uptime' }
];

/** POST /api/reports — generalizes the one-off generateReport() on DashboardComponent. */
@Component({
  selector: 'app-report-request-form',
  standalone: false,
  templateUrl: './report-request-form.component.html',
  styleUrls: ['./report-request-form.component.css']
})
export class ReportRequestFormComponent {
  reportTypeOptions = REPORT_TYPE_OPTIONS;

  reportType: ReportType = 'MONTHLY_FINES';
  format: ReportFormat = 'PDF';
  periodStart = '';
  periodEnd = '';

  submitting = false;
  submitError = '';
  result: ReportDto | null = null;

  constructor(private reportService: ReportService, private router: Router) {}

  get canSubmit(): boolean {
    return !!this.periodStart && !!this.periodEnd && !this.submitting;
  }

  submit(): void {
    if (!this.canSubmit) return;

    this.submitting = true;
    this.submitError = '';

    const request: ReportRequestDto = {
      reportType: this.reportType,
      format: this.format,
      periodStart: this.periodStart,
      periodEnd: this.periodEnd
    };

    this.reportService.requestReport(request).subscribe({
      next: (response) => {
        this.submitting = false;
        this.result = response.data;
      },
      error: (err) => {
        this.submitting = false;
        this.submitError = err?.error?.message ?? 'Unable to request this report.';
      }
    });
  }

  goToReports(): void {
    this.router.navigate(['/officer/reports']);
  }
}