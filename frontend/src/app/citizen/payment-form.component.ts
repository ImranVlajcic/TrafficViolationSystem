import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common'; // <-- Fixes NG8004 (pipes: number, lowercase, date)
import { FormsModule } from '@angular/forms';     // <-- Fixes NG8002 (ngModel, ngValue)

// Adjust these import paths to match your actual project layout.
import { FineService } from '../services/fine.service';
import { PaymentService } from '../services/payment.service';
import { FineDto } from '../models/fine.model';
import { PaymentDto } from '../models/payment.model';
import { PaymentMethod } from '../models/enums';

const METHOD_OPTIONS: { value: PaymentMethod; label: string }[] = [
  { value: 'CREDIT_CARD', label: 'Credit card' },
  { value: 'DEBIT_CARD', label: 'Debit card' },
  { value: 'BANK_TRANSFER', label: 'Bank transfer' },
  { value: 'CASH', label: 'Cash' },
  { value: 'ONLINE_PORTAL', label: 'Online portal' }
];

/**
 * POST /api/payments for a single fine, reached via /citizen/payments/pay/:fineId
 * (linked from both fine-list and fine-detail).
 *
 * Deliberately has no amount field: the backend derives it from
 * fine.totalDue server-side, so this only ever collects the method and
 * an optional note. Loads the fine first so the citizen sees what
 * they're actually paying, and blocks submission if it's already
 * PAID/CANCELLED.
 */
@Component({
  selector: 'app-payment-form',
  standalone: false,
  templateUrl: './payment-form.component.html',
  styleUrls: ['./payment-form.component.css']
})
export class PaymentFormComponent implements OnInit {
  fine: FineDto | null = null;
  loadingFine = false;
  loadError = '';

  method: PaymentMethod = 'CREDIT_CARD';
  notes = '';
  methodOptions = METHOD_OPTIONS;

  submitting = false;
  submitError = '';
  result: PaymentDto | null = null;

  downloadingReceipt = false;
  receiptError = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fineService: FineService,
    private paymentService: PaymentService
  ) {}

  ngOnInit(): void {
    const fineId = this.route.snapshot.paramMap.get('fineId');
    if (!fineId) {
      this.loadError = 'No fine specified.';
      return;
    }
    this.loadFine(fineId);
  }

  private loadFine(fineId: string): void {
    this.loadingFine = true;
    this.loadError = '';

    this.fineService.findById(fineId).subscribe({
      next: (fine) => {
        this.fine = fine;
        this.loadingFine = false;
        if (fine.status !== 'UNPAID' && fine.status !== 'OVERDUE') {
          this.loadError = `This fine is already ${fine.status.toLowerCase()} — nothing to pay.`;
        }
      },
      error: () => {
        this.loadError = 'Unable to load this fine.';
        this.loadingFine = false;
      }
    });
  }

  get canSubmit(): boolean {
    return !!this.fine && !this.loadError && !this.submitting;
  }

  submit(): void {
    if (!this.fine) return;

    this.submitting = true;
    this.submitError = '';

    this.paymentService
      .pay({ fineId: this.fine.id, method: this.method, notes: this.notes || undefined })
      .subscribe({
        next: (response) => {
          this.submitting = false;
          this.result = response.data;
        },
        error: (err) => {
          this.submitting = false;
          this.submitError = err?.error?.message ?? 'Payment could not be processed.';
        }
      });
  }

  /**
   * receiptPdfFile being present is the closest thing PaymentDto has to
   * a "receiptReady" flag — receipt generation is async on the backend,
   * so right after a successful payment this is often still empty.
   */
  get canDownloadReceipt(): boolean {
    return !!this.result?.receiptPdfFile;
  }

  downloadReceipt(): void {
    if (!this.result) return;
    this.receiptError = '';
    this.downloadingReceipt = true;

    this.paymentService.downloadReceipt(this.result.id).subscribe({
      next: (blob) => {
        this.downloadingReceipt = false;
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `receipt-${this.result!.transactionId}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.downloadingReceipt = false;
        this.receiptError = 'Receipt not ready yet — check your payments list shortly.';
      }
    });
  }

  goToPayments(): void {
    this.router.navigate(['/citizen/payments']);
  }

  goToFine(): void {
    if (this.fine) {
      this.router.navigate(['/citizen/fines', this.fine.id]);
    }
  }
}
