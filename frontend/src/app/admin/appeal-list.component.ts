import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { AppealService } from '../services/appeal.service';
import { AppealDto, AppealSearchObject } from '../models/appeal.model';
import { AppealStatus } from '../models/enums';
import { DataTableColumn } from '../shared/data-table/data-table.component';

const STATUS_LABELS: Record<AppealStatus, string> = {
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  WITHDRAWN: 'Withdrawn'
};

const STATUS_OPTIONS: AppealStatus[] = [
  'SUBMITTED',
  'UNDER_REVIEW',
  'APPROVED',
  'REJECTED',
  'WITHDRAWN'
];

@Component({
  selector: 'app-admin-appeal-list',
  standalone: false,
  templateUrl: './appeal-list.component.html',
  styleUrls: ['./appeal-list.component.css']
})
export class AdminAppealListComponent implements OnInit {
  appeals: AppealDto[] = [];
  loading = false;
  error = '';

  statusOptions = STATUS_OPTIONS;
  statusLabels = STATUS_LABELS;

  filters: AppealSearchObject = {};
  page = 0;
  size = 20;
  totalElements = 0;
  hasMore = false;

  columns: DataTableColumn[] = [
    {
      key: 'appealNumber',
      label: 'Appeal #',
      mono: true
    },
    {
      key: 'driverId',
      label: 'Driver',
      mono: true
    },
    {
      key: 'violationId',
      label: 'Violation',
      mono: true
    },
    {
      key: 'submittedAt',
      label: 'Submitted',
      format: (row) => new Date(row.submittedAt).toLocaleDateString()
    },
    {
      key: 'status',
      label: 'Status',
      align: 'center',
      format: (row) => STATUS_LABELS[row.status as AppealStatus] ?? row.status
    }
  ];

  constructor(
    private appealService: AppealService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';

    const searchObject: AppealSearchObject = {
      ...this.filters,
      page: this.page,
      size: this.size
    } as AppealSearchObject;

    this.appealService.search(searchObject).subscribe({
      next: (result: any) => {
        this.appeals = result?.resultList ?? [];
        this.totalElements = result?.count ?? 0;
        this.hasMore = result?.hasMore ?? false;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load appeals.';
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.page = 0;
    this.load();
  }

  resetFilters(): void {
    this.filters = {};
    this.page = 0;
    this.load();
  }

  nextPage(): void {
    if (!this.hasMore) return;

    this.page += 1;
    this.load();
  }

  prevPage(): void {
    if (this.page <= 0) return;

    this.page -= 1;
    this.load();
  }

  openAppeal(row: AppealDto): void {
    this.router.navigate(['/admin/appeals', row.id]);
  }
}