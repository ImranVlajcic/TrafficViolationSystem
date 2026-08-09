import { Component, OnInit } from '@angular/core';

// Adjust these import paths to match your actual project layout.
import { FineService } from '../services/fine.service';
import { FineDto, FineSearchObject } from '../models/fine.model';
import { FineStatus } from '../models/enums';
import { DataTableColumn, SortChange } from '../shared/data-table/data-table.component';
import { ConfirmDialogTone } from '../shared/confirm-dialog/confirm-dialog.component';

const STATUS_LABELS: Record<FineStatus, string> = {
  UNPAID: 'Unpaid',
  OVERDUE: 'Overdue',
  DISPUTED: 'Disputed',
  PAID: 'Paid',
  CANCELLED: 'Cancelled'
};

export const FINE_STATUS_OPTIONS: { value: FineStatus; label: string }[] = (
  Object.keys(STATUS_LABELS) as FineStatus[]
).map((value) => ({ value, label: STATUS_LABELS[value] }));

// Same cancel-eligibility assumption as the officer fine-list (not
// backend-confirmed) — verify against FineService.java's actual
// status-transition guard before relying on this.
const CANCELLABLE_STATUSES: FineStatus[] = ['UNPAID', 'OVERDUE', 'DISPUTED'];

const PAGE_SIZE = 10;

interface FineFilters {
  search: string;
  status: FineStatus | '';
  driverId: string;
  violationId: string;
  issuedById: string;
  issuedFrom: string;
  issuedTo: string;
  overdueDatePassed: boolean;
}

function emptyFilters(): FineFilters {
  return {
    search: '',
    status: '',
    driverId: '',
    violationId: '',
    issuedById: '',
    issuedFrom: '',
    issuedTo: '',
    overdueDatePassed: false
  };
}

/**
 * Admin-wide fine browser. Unlike the officer fine-list (scoped to
 * issuedById via GET /api/users/me), admins aren't restricted to their
 * own fines, so every FineSearchObject filter is exposed directly here —
 * no officer-profile lookup needed before the first load.
 *
 * Same no-create/no-edit rule as FineService: fines are issued
 * automatically when a violation is confirmed, so the only row action
 * is cancel (OFFICER/ADMIN per FineService's own doc comment).
 */
@Component({
  selector: 'app-admin-fine-list',
  standalone: false,
  templateUrl: './fine-list.component.html'
})
export class AdminFineListComponent implements OnInit {
  fines: FineDto[] = [];
  loading = false;
  error = '';

  page = 1;
  hasMore = false;
  count = 0;

  sortColumn: string | null = 'issuedAt';
  sortDirection: 'ASC' | 'DESC' = 'DESC';

  filters: FineFilters = emptyFilters();
  statusOptions = FINE_STATUS_OPTIONS;

  reasonDraft: Record<string, string> = {};
  actionError = '';
  processingId: string | null = null;

  confirmOpen = false;
  confirmTone: ConfirmDialogTone = 'danger';
  confirmTitle = '';
  confirmMessage = '';
  private pendingRow: FineDto | null = null;

  columns: DataTableColumn<FineDto>[] = [
    { key: 'fineNumber', label: 'Fine #', mono: true },
    {
      key: 'issuedAt',
      label: 'Issued',
      sortable: true,
      format: (row) => new Date(row.issuedAt).toLocaleDateString()
    },
    {
      key: 'dueDate',
      label: 'Due',
      sortable: true,
      format: (row) => new Date(row.dueDate).toLocaleDateString()
    },
    {
      key: 'totalDue',
      label: 'Total due',
      align: 'right',
      format: (row) => `${row.totalDue.toFixed(2)} ${row.currency}`
    },
    { key: 'penaltyPoints', label: 'Points', align: 'center' },
    {
      key: 'status',
      label: 'Status',
      align: 'center',
      format: (row) => STATUS_LABELS[row.status] ?? row.status
    }
  ];

  constructor(private fineService: FineService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';

    const searchObject: FineSearchObject = {
      page: this.page,
      limit: PAGE_SIZE,
      order: this.sortColumn ?? undefined,
      orderDirection: this.sortDirection,
      includeCount: true,
      ...this.buildFilterParams()
    };

    this.fineService.search(searchObject).subscribe({
      next: (result) => {
        this.fines = result?.resultList ?? [];
        this.hasMore = result?.hasMore ?? false;
        this.count = result?.count ?? 0;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load fines.';
        this.loading = false;
      }
    });
  }

  private buildFilterParams(): Partial<FineSearchObject> {
    const f = this.filters;
    const params: Partial<FineSearchObject> = {};
    if (f.search.trim()) params.search = f.search.trim();
    if (f.status) params.status = f.status;
    if (f.driverId.trim()) params.driverId = f.driverId.trim();
    if (f.violationId.trim()) params.violationId = f.violationId.trim();
    if (f.issuedById.trim()) params.issuedById = f.issuedById.trim();
    if (f.issuedFrom) params.issuedFrom = f.issuedFrom;
    if (f.issuedTo) params.issuedTo = f.issuedTo;
    if (f.overdueDatePassed) params.overdueDatePassed = true;
    return params;
  }

  applyFilters(): void {
    this.page = 1;
    this.load();
  }

  resetFilters(): void {
    this.filters = emptyFilters();
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

  canCancel(row: FineDto): boolean {
    return CANCELLABLE_STATUSES.includes(row.status);
  }

  canSubmitCancel(row: FineDto): boolean {
    return !!this.reasonDraft[row.id]?.trim();
  }

  requestCancel(row: FineDto): void {
    this.actionError = '';
    this.pendingRow = row;
    this.confirmTitle = 'Cancel this fine?';
    this.confirmMessage = `${row.fineNumber} will be cancelled and its penalty points reversed. This can't be undone.`;
    this.confirmOpen = true;
  }

  onDialogConfirmed(): void {
    this.confirmOpen = false;
    if (!this.pendingRow) {
      return;
    }

    const row = this.pendingRow;
    const reason = this.reasonDraft[row.id]?.trim() ?? '';

    this.processingId = row.id;
    this.actionError = '';

    this.fineService.cancel(row.id, reason).subscribe({
      next: () => {
        this.processingId = null;
        delete this.reasonDraft[row.id];
        this.load();
      },
      error: () => {
        this.processingId = null;
        this.actionError = 'Unable to cancel this fine.';
      }
    });
  }

  onDialogCancelled(): void {
    this.confirmOpen = false;
    this.pendingRow = null;
  }
}
