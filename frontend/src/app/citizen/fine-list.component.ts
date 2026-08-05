import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common'; // <-- Fixes NG8004 (pipes: number, lowercase, date)
import { FormsModule } from '@angular/forms';     // <-- Fixes NG8002 (ngModel, ngValue)

// Adjust these import paths to match your actual project layout.
import { FineService } from '../services/fine.service';
import { FineDto } from '../models/fine.model';
import { FineStatus } from '../models/enums';
import { DataTableColumn, SortChange, SortDirection } from '../shared/data-table/data-table.component';

interface StatusOption {
  value: FineStatus | '';
  label: string;
}

const STATUS_OPTIONS: StatusOption[] = [
  { value: '', label: 'All statuses' },
  { value: 'UNPAID', label: 'Unpaid' },
  { value: 'OVERDUE', label: 'Overdue' },
  { value: 'DISPUTED', label: 'Disputed' },
  { value: 'PAID', label: 'Paid' },
  { value: 'CANCELLED', label: 'Cancelled' }
];

/**
 * Full list of the signed-in citizen's fines.
 *
 * Deliberately uses FineService.getMyFines() (GET /api/fines/my) rather
 * than search(): that's the one endpoint the docs actually confirm as
 * citizen-scoped, and it returns the *complete* array for the current
 * citizen (ApiResponse<FineDto[]>) with no paging metadata at all — the
 * citizen also has no driverId available client-side to filter search()
 * with. So everything below (status filter, text search, column sort)
 * runs in-memory against that one loaded array instead of round-tripping
 * to the backend.
 */
@Component({
  selector: 'app-fine-list',
  standalone: false,
  templateUrl: './fine-list.component.html',
  styleUrls: ['./fine-list.component.css']
})
export class FineListComponent implements OnInit {
  fines: FineDto[] = [];
  loading = false;
  error = '';

  statusOptions = STATUS_OPTIONS;
  statusFilter: FineStatus | '' = '';
  searchTerm = '';

  sortColumn: string | null = 'issuedAt';
  sortDirection: SortDirection = 'DESC';

  columns: DataTableColumn<FineDto>[] = [
    { key: 'fineNumber', label: 'Fine #', mono: true, sortable: true },
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
      mono: true,
      sortable: true,
      format: (row) => `${row.totalDue.toFixed(2)} ${row.currency}`
    },
    { key: 'status', label: 'Status', align: 'center', sortable: true }
  ];

  constructor(private fineService: FineService, private router: Router) {}

  ngOnInit(): void {
    this.loadFines();
  }

  loadFines(): void {
    this.loading = true;
    this.error = '';

    this.fineService.getMyFines().subscribe({
      next: (response) => {
        this.fines = response.data ?? [];
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load your fines.';
        this.loading = false;
      }
    });
  }

  /** Filtered + sorted view handed to the table — recomputed on every change detection pass. */
  get rows(): FineDto[] {
    let result = this.fines;

    if (this.statusFilter) {
      result = result.filter((f) => f.status === this.statusFilter);
    }

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.trim().toLowerCase();
      result = result.filter((f) => f.fineNumber.toLowerCase().includes(term));
    }

    if (this.sortColumn) {
      const column = this.sortColumn as keyof FineDto;
      const direction = this.sortDirection === 'ASC' ? 1 : -1;
      result = [...result].sort((a, b) => {
        const va = a[column];
        const vb = b[column];
        if (va === vb) return 0;
        return (va as any) > (vb as any) ? direction : -direction;
      });
    }

    return result;
  }

  onSortChange(change: SortChange): void {
    this.sortColumn = change.column;
    this.sortDirection = change.direction;
  }

  onRowClick(row: FineDto): void {
    this.router.navigate(['/citizen/fines', row.id]);
  }

  payNow(row: FineDto): void {
    this.router.navigate(['/citizen/payments/pay', row.id]);
  }

  canPay(row: FineDto): boolean {
    return row.status === 'UNPAID' || row.status === 'OVERDUE';
  }
}
