import { Component, OnInit } from '@angular/core';

// Adjust these import paths to match your actual project layout.
import { FineService } from '../services/fine.service';
import { UserService } from '../services/user.service';
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

// FineService.cancel() reverses penalty points and can't be applied to a
// paid fine (per its own doc comment). CANCELLED is excluded as already
// terminal. DISPUTED is included on the assumption an officer can still
// cancel a disputed fine directly — not backend-confirmed, verify against
// FineService.java's actual status-transition guard.
const CANCELLABLE_STATUSES: FineStatus[] = ['UNPAID', 'OVERDUE', 'DISPUTED'];

const PAGE_SIZE = 10;

/**
 * Fines tied to violations the officer handled. There's no dedicated
 * "my issued fines" endpoint — this uses the generic GET /api/fines with
 * FineSearchObject.issuedById, the same GET /api/users/me-for-id pattern
 * as citizen appeal-list, since issuedById expects the officer's own
 * user id.
 */
@Component({
  selector: 'app-fine-list',
  standalone: false,
  templateUrl: './fine-list.component.html'
})
export class FineListComponent implements OnInit {
  fines: FineDto[] = [];
  loading = false;
  error = '';

  page = 1;
  hasMore = false;
  count = 0;

  sortColumn: string | null = 'issuedAt';
  sortDirection: 'ASC' | 'DESC' = 'DESC';

  reasonDraft: Record<string, string> = {};
  actionError = '';
  processingId: string | null = null;

  confirmOpen = false;
  confirmTone: ConfirmDialogTone = 'danger';
  confirmTitle = '';
  confirmMessage = '';
  private pendingRow: FineDto | null = null;

  private officerId: string | null = null;

  columns: DataTableColumn<FineDto>[] = [
    { key: 'fineNumber', label: 'Fine #', mono: true },
    {
      key: 'issuedAt',
      label: 'Issued',
      sortable: true,
      format: (row) => new Date(row.issuedAt).toLocaleDateString()
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

  constructor(
    private fineService: FineService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.userService.getProfile().subscribe({
      next: (profileResponse) => {
        this.officerId = profileResponse.data.id;
        this.load();
      },
      error: () => {
        this.error = 'Unable to load your profile.';
        this.loading = false;
      }
    });
  }

  load(): void {
    if (!this.officerId) {
      return;
    }
    this.loading = true;
    this.error = '';

    const searchObject: FineSearchObject = {
      issuedById: this.officerId,
      page: this.page,
      limit: PAGE_SIZE,
      order: this.sortColumn ?? undefined,
      orderDirection: this.sortDirection,
      includeCount: true
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
