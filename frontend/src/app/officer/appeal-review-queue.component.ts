import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { AppealService } from '../services/appeal.service';
import { AppealDto } from '../models/appeal.model';
import { AppealStatus } from '../models/enums';
import { DataTableColumn } from '../shared/data-table/data-table.component';

const STATUS_LABELS: Record<AppealStatus, string> = {
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  WITHDRAWN: 'Withdrawn'
};

/**
 * Officer-facing appeal queue — GET /api/appeals/pending returns only
 * SUBMITTED appeals, oldest first (per AppealService.getPendingQueue doc).
 * Row click opens the review detail, since start-review/approve/reject are
 * single-record actions rather than bulk table actions.
 */
@Component({
  selector: 'app-appeal-review-queue',
  standalone: false,
  templateUrl: './appeal-review-queue.component.html',
  styleUrls: ['./appeal-review-queue.component.css']
})
export class AppealReviewQueueComponent implements OnInit {
  appeals: AppealDto[] = [];
  loading = false;
  error = '';

  columns: DataTableColumn<AppealDto>[] = [
    { key: 'appealNumber', label: 'Appeal #', mono: true },
    {
      key: 'submittedAt',
      label: 'Submitted',
      format: (row) => new Date(row.submittedAt).toLocaleDateString()
    },
    { key: 'reason', label: 'Reason', format: (row) => this.truncate(row.reason, 70) },
    {
      key: 'status',
      label: 'Status',
      align: 'center',
      format: (row) => STATUS_LABELS[row.status] ?? row.status
    }
  ];

  constructor(private appealService: AppealService, private router: Router) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';

    this.appealService.getPendingQueue().subscribe({
      next: (response) => {
        this.appeals = response.data ?? [];
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load pending appeals.';
        this.loading = false;
      }
    });
  }

  openReview(row: AppealDto): void {
    this.router.navigate(['/officer/appeals', row.id]);
  }

  private truncate(value: string, max: number): string {
    return value.length > max ? `${value.slice(0, max - 1)}…` : value;
  }
}