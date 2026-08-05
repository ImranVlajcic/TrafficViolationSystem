import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { ReportService } from '../services/report.service';
import { ReportDto } from '../models/report.model';
import { ReportStatus } from '../models/enums';
import { DataTableColumn } from '../shared/data-table/data-table.component';

const STATUS_LABELS: Record<ReportStatus, string> = {
  PENDING: 'Pending',
  GENERATED: 'Generated',
  DONE: 'Ready',
  FAILED: 'Failed'
};

/**
 * GET /api/reports/my — the officer's own report request history.
 * NOTE: ReportDto (report.model.ts) has no `ready` boolean, despite
 * report.service.ts's doc comments referencing one — status === 'DONE'
 * is used as the download-readiness check instead. Verify against the
 * real API response if downloads unexpectedly 404.
 */
@Component({
  selector: 'app-report-list',
  standalone: false,
  templateUrl: './report-list.component.html',
  styleUrls: ['./report-list.component.css']
})
export class ReportListComponent implements OnInit {
  reports: ReportDto[] = [];
  loading = false;
  error = '';

  downloadingId: string | null = null;
  downloadError = '';

  columns: DataTableColumn<ReportDto>[] = [
    { key: 'reportType', label: 'Type' },
    { key: 'format', label: 'Format', align: 'center' },
    {
      key: 'periodStart',
      label: 'Period',
      format: (row) =>
        `${new Date(row.periodStart).toLocaleDateString()} – ${new Date(row.periodEnd).toLocaleDateString()}`
    },
    {
      key: 'status',
      label: 'Status',
      align: 'center',
      format: (row) => STATUS_LABELS[row.status] ?? row.status
    }
  ];

  constructor(private reportService: ReportService, private router: Router) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';

    this.reportService.getMyReports().subscribe({
      next: (response) => {
        this.reports = response.data ?? [];
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load your reports.';
        this.loading = false;
      }
    });
  }

  isReady(row: ReportDto): boolean {
    return row.status === 'DONE';
  }

  download(row: ReportDto): void {
    this.downloadError = '';
    this.downloadingId = row.id;

    this.reportService.downloadFile(row.id).subscribe({
      next: (blob) => {
        this.downloadingId = null;
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${row.reportType.toLowerCase()}-${row.id}.${row.format.toLowerCase()}`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.downloadingId = null;
        this.downloadError = 'Unable to download this report.';
      }
    });
  }

  requestNew(): void {
    this.router.navigate(['/officer/reports/new']);
  }
}