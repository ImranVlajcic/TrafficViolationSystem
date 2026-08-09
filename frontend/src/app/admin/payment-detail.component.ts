import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { PaymentService } from '../services/payment.service';
import { PaymentDto } from '../models/payment.model';

/**
 * Admin payment detail — read-only (payments are immutable, no
 * admin actions exist per PaymentService). Combines the single payment
 * record with a secondary "other attempts for this fine" panel via
 * getForFine(fineId), same dual-panel reasoning used for
 * camera-maintenance and vehicle-transfer. Receipt download only offered
 * when receiptPdfFile is truthy, same readiness-signal workaround used
 * elsewhere (PaymentDto has no receiptReady field despite the service's
 * doc comment referencing one — verify against the real entity).
 */
@Component({
  selector: 'app-admin-payment-detail',
  standalone: false,
  templateUrl: './payment-detail.component.html',
  styleUrls: ['./payment-detail.component.css']
})
export class AdminPaymentDetailComponent implements OnInit {
  payment: PaymentDto | null = null;
  loading = false;
  error = '';

  relatedPayments: PaymentDto[] = [];
  relatedLoading = false;
  relatedError = '';

  downloading = false;
  downloadError = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  private get id(): string {
    return this.route.snapshot.paramMap.get('id')!;
  }

  load(): void {
    this.loading = true;
    this.error = '';

    this.paymentService.findById(this.id).subscribe({
      next: (payment) => {
        this.payment = payment;
        this.loading = false;
        this.loadRelated(payment.fineId);
      },
      error: () => {
        this.error = 'Unable to load this payment.';
        this.loading = false;
      }
    });
  }

  private loadRelated(fineId: string): void {
    this.relatedLoading = true;
    this.relatedError = '';

    this.paymentService.getForFine(fineId).subscribe({
      next: (response) => {
        this.relatedPayments = (response.data ?? []).filter((p) => p.id !== this.payment?.id);
        this.relatedLoading = false;
      },
      error: () => {
        this.relatedError = 'Unable to load other attempts for this fine.';
        this.relatedLoading = false;
      }
    });
  }

  get canDownloadReceipt(): boolean {
    return !!this.payment?.receiptPdfFile;
  }

  downloadReceipt(): void {
    if (!this.payment) return;
    this.downloading = true;
    this.downloadError = '';

    this.paymentService.downloadReceipt(this.payment.id).subscribe({
      next: (blob) => {
        this.downloading = false;
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `receipt-${this.payment?.transactionId}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.downloading = false;
        this.downloadError = 'Receipt is not ready yet.';
      }
    });
  }

  openRelated(row: PaymentDto): void {
    this.router.navigate(['/admin/payments', row.id]);
  }

  back(): void {
    this.router.navigate(['/admin/payments']);
  }
}
