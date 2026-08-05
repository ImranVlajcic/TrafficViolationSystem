import { Component, OnInit } from '@angular/core';

import { JobService } from '../services/job.service';
import { JobExecutionLogDto } from '../models/job.model';
import { DataTableColumn } from '../shared/data-table/data-table.component';

type LogFilter = 'all' | 'stuck' | 'failed';

/**
 * Routed /admin/jobs page. Combines the manual trigger grid with the
 * execution log history on one page — same pattern as camera-maintenance
 * combining its history table + log form + offline panel.
 *
 * Status values come from enums.ts's JobStatus, which flags 'SUCCESS' as
 * an unconfirmed guess (the doc literally shows "SUCCES") — worth a quick
 * check against JobStatus.java before relying on that value anywhere else.
 */
@Component({
  selector: 'app-job-list',
  standalone: false,
  templateUrl: './job-list.component.html',
  styleUrls: ['./job-list.component.css']
})
export class JobListComponent implements OnInit {
  columns: DataTableColumn<JobExecutionLogDto>[] = [
    { key: 'jobName', label: 'Job' },
    { key: 'status', label: 'Status' },
    { key: 'startedAt', label: 'Started', mono: true, format: (row) => this.formatDate(row.startedAt) },
    { key: 'finishedAt', label: 'Finished', mono: true, format: (row) => this.formatDate(row.finishedAt) },
    {
      key: 'recordsProcessed',
      label: 'Records',
      align: 'right',
      format: (row) => (row.recordsProcessed ?? undefined)?.toString() ?? '—'
    },
    { key: 'triggeredBy', label: 'Triggered by' }
  ];

  rows: JobExecutionLogDto[] = [];
  loading = false;
  error: string | null = null;

  jobNameFilter = '';
  activeFilter: LogFilter = 'all';

  selectedLog: JobExecutionLogDto | null = null;

  constructor(private readonly jobService: JobService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.selectedLog = null;

    const request$ =
      this.activeFilter === 'stuck'
        ? this.jobService.getStuckJobs()
        : this.activeFilter === 'failed'
        ? this.jobService.getFailedJobs()
        : this.jobService.getLogs(this.jobNameFilter || undefined);

    request$.subscribe({
      next: (logs) => {
        this.rows = logs;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load job execution logs.';
        this.loading = false;
      }
    });
  }

  setFilter(filter: LogFilter): void {
    this.activeFilter = filter;
    this.load();
  }

  onSearch(): void {
    this.activeFilter = 'all';
    this.load();
  }

  onResetJobName(): void {
    this.jobNameFilter = '';
    this.load();
  }

  onRowClick(row: JobExecutionLogDto): void {
    this.selectedLog = this.selectedLog?.id === row.id ? null : row;
  }

  closeDetail(): void {
    this.selectedLog = null;
  }

  private formatDate(value?: string): string {
    if (!value) {
      return '—';
    }
    const date = new Date(value);
    return isNaN(date.getTime()) ? value : date.toLocaleString();
  }
}