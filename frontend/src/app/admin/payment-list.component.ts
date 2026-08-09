import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { PaymentService } from '../services/payment.service';
import { PaymentDto, PaymentSearchObject } from '../models/payment.model';
import { PaymentMethod, PaymentStatus } from '../models/enums';
import { DataTableColumn } from '../shared/data-table/data-table.component';

const STATUS_LABELS: Record<PaymentStatus, string> = {
  PENDING: 'Pending',
  SUCCESS: 'Success',
  FAILED: 'Failed',
  REFUNDED: 'Refunded',
  REVERSED: 'Reversed'
};

const STATUS_OPTIONS: PaymentStatus[] = ['PENDING', 'SUCCESS', 'FAILED', 'REFUNDED', 'REVERSED'];

const METHOD_LABELS: Record<PaymentMethod, string> = {
  CREDIT_CARD: 'Credit card',
  DEBIT_CARD: 'Debit card',
  BANK_TRANSFER: 'Bank transfer',
  CASH: 'Cash',
  ONLINE_PORTAL: 'Online portal'
};

const METHOD_OPTIONS: PaymentMethod[] = ['CREDIT_CARD', 'DEBIT_CARD', 'BANK_TRANSFER', 'CASH', 'ONLINE_PORTAL'];

/**
 * Admin-facing payment list. PaymentService.search() returns
 * { resultList, count, hasMore } — same shape as DriverService.search(),
 * NOT a Spring-style Page<T>. count can be null (server doesn't always
 * compute it), so don't treat null as zero.
 */
@Component({
  selector: 'app-admin-payment-list',
  standalone: false,
  templateUrl: './payment-list.component.html',
  styleUrls: ['./payment-list.component.css']
})
export class AdminPaymentListComponent implements OnInit {
  payments: PaymentDto[] = [];
  loading = false;
  error = '';

  statusOptions = STATUS_OPTIONS;
  statusLabels = STATUS_LABELS;
  methodOptions = METHOD_OPTIONS;
  methodLabels = METHOD_LABELS;

  filters: PaymentSearchObject = {};
  page = 0;
  size = 20;
  totalElements: number | null = null;
  hasMore = false;

  columns: DataTableColumn<PaymentDto>[] = [
    { key: 'transactionId', label: 'Transaction', mono: true },
    { key: 'fineId', label: 'Fine', mono: true },
    {
      key: 'method',
      label: 'Method',
      format: (row) => METHOD_LABELS[row.method] ?? row.method
    },
    {
      key: 'amount',
      label: 'Amount',
      align: 'right',
      format: (row) => `${row.amount.toFixed(2)} ${row.currency}`
    },
    {
      key: 'status',
      label: 'Status',
      align: 'center',
      format: (row) => STATUS_LABELS[row.status] ?? row.status
    },
    {
      key: 'paidAt',
      label: 'Paid',
      format: (row) => (row.paidAt ? new Date(row.paidAt).toLocaleDateString() : '—')
    }
  ];

  constructor(private paymentService: PaymentService, private router: Router) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';

    const searchObject: PaymentSearchObject = {
      ...this.filters,
      page: this.page,
      size: this.size
    } as PaymentSearchObject;

    this.paymentService.search(searchObject).subscribe({
      next: (result: any) => {
        this.payments = result?.resultList ?? [];
        // count can be null — only use it when the server actually provides it
        this.totalElements = result?.count ?? null;
        this.hasMore = result?.hasMore ?? false;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load payments.';
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

  openPayment(row: PaymentDto): void {
    this.router.navigate(['/admin/payments', row.id]);
  }
}