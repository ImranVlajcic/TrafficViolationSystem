import { Component, OnInit } from '@angular/core';

import { AuditLogService } from '../services/audit-log.service';
import { AuditLogDto, AuditLogSearchObject } from '../models/auditlog.model';
import { DataTableColumn, SortChange } from '../shared/data-table/data-table.component';

const PAGE_LIMIT = 20;

interface AuditLogFilters {
  action: string;
  entityType: string;
  entityId: string;
  actorId: string;
  fromDate: string;
  toDate: string;
}

const EMPTY_FILTERS: AuditLogFilters = {
  action: '',
  entityType: '',
  entityId: '',
  actorId: '',
  fromDate: '',
  toDate: ''
};

@Component({
  selector: 'app-audit-log-list',
  standalone: false,
  templateUrl: './audit-log-list.component.html',
  styleUrls: ['./audit-log-list.component.css']
})
export class AuditLogListComponent implements OnInit {
  columns: DataTableColumn<AuditLogDto>[] = [
    { key: 'occuredAt', label: 'Occurred', sortable: true, mono: true, format: (row) => this.formatDate(row.occuredAt) },
    { key: 'action', label: 'Action', sortable: true },
    { key: 'entityType', label: 'Entity', sortable: true, format: (row) => `${row.entityType} · ${row.entityId}` },
    { key: 'actorUsername', label: 'Actor', sortable: true }
  ];

  rows: AuditLogDto[] = [];
  loading = false;
  error: string | null = null;

  page = 1;
  hasMore = false;
  count = 0;

  sortColumn: string | null = 'occuredAt';
  sortDirection: 'ASC' | 'DESC' = 'DESC';

  selectedLog: AuditLogDto | null = null;

  filters: AuditLogFilters = { ...EMPTY_FILTERS };

  constructor(private readonly auditLogService: AuditLogService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;

    const searchObject: AuditLogSearchObject = {
      page: this.page,
      limit: PAGE_LIMIT,
      includeCount: true,
      order: this.sortColumn ?? undefined,
      orderDirection: this.sortDirection,
      action: this.filters.action || undefined,
      entityType: this.filters.entityType || undefined,
      entityId: this.filters.entityId || undefined,
      actorId: this.filters.actorId || undefined,
      fromDate: this.filters.fromDate || undefined,
      toDate: this.filters.toDate || undefined
    };

    this.auditLogService.search(searchObject).subscribe({
      next: (result) => {
        this.rows = result.resultList;
        this.hasMore = result.hasMore;
        this.count = result.count ?? this.rows.length;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load audit logs.';
        this.loading = false;
      }
    });
  }

  onSearch(): void {
    this.page = 1;
    this.load();
  }

  onReset(): void {
    this.filters = { ...EMPTY_FILTERS };
    this.page = 1;
    this.load();
  }

  onPageChange(page: number): void {
    this.page = page;
    this.load();
  }

  onSortChange(sort: SortChange): void {
    this.sortColumn = sort.column;
    this.sortDirection = sort.direction;
    this.page = 1;
    this.load();
  }

  onRowClick(row: AuditLogDto): void {
    this.selectedLog = this.selectedLog?.id === row.id ? null : row;
  }

  closeDetail(): void {
    this.selectedLog = null;
  }

  private formatDate(value: string): string {
    if (!value) {
      return '—';
    }
    const date = new Date(value);
    return isNaN(date.getTime()) ? value : date.toLocaleString();
  }
}