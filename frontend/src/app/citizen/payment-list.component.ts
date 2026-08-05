import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; // <-- Fixes NG8004 (pipes: number, lowercase, date)
import { FormsModule } from '@angular/forms';     // <-- Fixes NG8002 (ngModel, ngValue)

// Adjust these import paths to match your actual project layout.
import { PaymentService } from '../services/payment.service';
import { PaymentDto, PaymentSearchObject } from '../models/payment.model';
import { PaymentMethod, PaymentStatus } from '../models/enums';
import { DataTableColumn, SortChange, SortDirection } from '../shared/data-table/data-table.component';

const STATUS_OPTIONS: { value: PaymentStatus | ''; label: string }[] = [
  { value: '', label: 'All statuses' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'SUCCESS', label: 'Success' },
  { value: 'FAILED', label: 'Failed' },
  { value: 'REFUNDED', label: 'Refunded' },
  { value: 'REVERSED', label: 'Reversed' }
];

const METHOD_OPTIONS: { value: PaymentMethod | ''; label: string }[] = [
  { value: '', label: 'All methods' },
  { value: 'CREDIT_CARD', label: 'Credit card' },
  { value: 'DEBIT_CARD', label: 'Debit card' },
  { value: 'BANK_TRANSFER', label: 'Bank transfer' },
  { value: 'CASH', label: 'Cash' },
  { value: 'ONLINE_PORTAL', label: 'Online portal' }
];

const PAGE_SIZE = 10;

/**
 * Citizen's own payment history.
 *
 * Unlike fine-list, GET /api/payments/my *is* wrapped in
 * ApiResponse<PagedResult<PaymentDto>> and does accept a full
 * PaymentSearchObject (page/limit/order/orderDirection + status/method
 * filters) — so paging, sorting, and filtering are all delegated to the
 * backend here instead of done in-memory.
 */
@Component({
  selector: 'app-payment-list',
  standalone: false,
  templateUrl: './payment-list.component.html',
  styleUrls: ['./payment-list.component.css']
})
export class PaymentListComponent implements OnInit {
  payments: PaymentDto[] = [];
  loading = false;
  error = '';

  page = 1;
  hasMore = false;
  count = 0;

  statusOptions = STATUS_OPTIONS;
  methodOptions = METHOD_OPTIONS;
  statusFilter: PaymentStatus | '' = '';
  methodFilter: PaymentMethod | '' = '';

  sortColumn: string | null = 'paidAt';
  sortDirection: SortDirection = 'DESC';

  columns: DataTableColumn<PaymentDto>[] = [
    { key: 'transactionId', label: 'Transaction', mono: true },
    { key: 'fineId', label: 'Fine', mono: true },
    { key: 'method', label: 'Method' },
    {
      key: 'amount',
      label: 'Amount',
      align: 'right',
      mono: true,
      sortable: true,
      format: (row) => `${row.amount.toFixed(2)} ${row.currency}`
    },
    { key: 'status', label: 'Status', align: 'center' },
    {
      key: 'paidAt',
      label: 'Paid',
      sortable: true,
      format: (row) => (row.paidAt ? new Date(row.paidAt).toLocaleDateString() : '—')
    }
  ];

  receiptErrors: Record<string, string> = {};
  downloadingReceipt: Record<string, boolean> = {};

  constructor(private paymentService: PaymentService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';

    const searchObject: PaymentSearchObject = {
      page: this.page,
      limit: PAGE_SIZE,
      includeCount: true,
      order: this.sortColumn ?? undefined,
      orderDirection: this.sortDirection,
      status: this.statusFilter || undefined,
      method: this.methodFilter || undefined
    };

    this.paymentService.getMyPayments(searchObject).subscribe({
      next: (response) => {
        const paged = response.data;
        this.payments = paged.resultList;
        this.hasMore = paged.hasMore;
        this.count = paged.count ?? this.count;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load your payments.';
        this.loading = false;
      }
    });
  }

  onPageChange(page: number): void {
    this.page = page;
    this.load();
  }

  onSortChange(change: SortChange): void {
    this.sortColumn = change.column;
    this.sortDirection = change.direction;
    this.page = 1;
    this.load();
  }

  onFilterChange(): void {
    this.page = 1;
    this.load();
  }

  canDownloadReceipt(row: PaymentDto): boolean {
    return row.status === 'SUCCESS' && !!row.receiptPdfFile;
  }

  downloadReceipt(row: PaymentDto): void {
    this.receiptErrors[row.id] = '';
    this.downloadingReceipt[row.id] = true;

    this.paymentService.downloadReceipt(row.id).subscribe({
      next: (blob) => {
        this.downloadingReceipt[row.id] = false;
        this.saveBlob(blob, `receipt-${row.transactionId}.pdf`);
      },
      error: () => {
        this.downloadingReceipt[row.id] = false;
        this.receiptErrors[row.id] = 'Receipt not ready yet.';
      }
    });
  }

  private saveBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  }
}
