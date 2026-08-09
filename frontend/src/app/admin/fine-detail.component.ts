import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { FineService } from '../services/fine.service';
import { FineDto } from '../models/fine.model';
import { FineStatus } from '../models/enums';
import { ConfirmDialogTone } from '../shared/confirm-dialog/confirm-dialog.component';

const STATUS_LABELS: Record<FineStatus, string> = {
  UNPAID: 'Unpaid',
  OVERDUE: 'Overdue',
  DISPUTED: 'Disputed',
  PAID: 'Paid',
  CANCELLED: 'Cancelled'
};

// Same cancel-eligibility assumption used across officer/admin fine-list.
const CANCELLABLE_STATUSES: FineStatus[] = ['UNPAID', 'OVERDUE', 'DISPUTED'];

/**
 * Single-fine admin view, routed /admin/fines/:id.
 *
 * FineService.findById()'s doc comment says this returns "full details
 * including violation reference" — but FineDto only exposes violationId,
 * no nested violation object, so that just means the id is present here;
 * it's linked out rather than rendered inline.
 *
 * PDF readiness: downloadPdf() 404s until the PDF is generated, and its
 * doc comment says to check `pdfReady` first, but FineDto has no such
 * field. Reuses the same pdfPath-truthiness workaround established for
 * the citizen fine-detail/payment components — unverified against
 * FineEntity.java.
 */
@Component({
  selector: 'app-admin-fine-detail',
  standalone: false,
  templateUrl: './fine-detail.component.html'
})
export class AdminFineDetailComponent implements OnInit {
  fine: FineDto | null = null;
  loading = false;
  error = '';

  downloadingPdf = false;
  pdfError = '';

  reasonDraft = '';
  actionError = '';
  processing = false;

  confirmOpen = false;
  confirmTone: ConfirmDialogTone = 'danger';
  confirmTitle = 'Cancel this fine?';
  confirmMessage = '';

  private fineId: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fineService: FineService
  ) {}

  ngOnInit(): void {
    this.fineId = this.route.snapshot.paramMap.get('id');
    if (!this.fineId) {
      this.error = 'Missing fine id.';
      return;
    }
    this.load();
  }

  get statusLabel(): string {
    if (!this.fine) return '';
    return STATUS_LABELS[this.fine.status] ?? this.fine.status;
  }

  get canCancel(): boolean {
    return !!this.fine && CANCELLABLE_STATUSES.includes(this.fine.status);
  }

  get canSubmitCancel(): boolean {
    return !!this.reasonDraft.trim();
  }

  get pdfReady(): boolean {
    return !!this.fine?.pdfPath;
  }

  load(): void {
    if (!this.fineId) return;
    this.loading = true;
    this.error = '';

    this.fineService.findById(this.fineId).subscribe({
      next: (fine) => {
        this.fine = fine;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load this fine.';
        this.loading = false;
      }
    });
  }

  downloadPdf(): void {
    if (!this.fine) return;
    this.downloadingPdf = true;
    this.pdfError = '';

    this.fineService.downloadPdf(this.fine.id).subscribe({
      next: (blob) => {
        this.downloadingPdf = false;
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${this.fine?.fineNumber ?? 'fine'}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.downloadingPdf = false;
        this.pdfError = 'Unable to download the PDF.';
      }
    });
  }

  requestCancel(): void {
    if (!this.fine) return;
    this.actionError = '';
    this.confirmMessage = `${this.fine.fineNumber} will be cancelled and its penalty points reversed. This can't be undone.`;
    this.confirmOpen = true;
  }

  onDialogConfirmed(): void {
    this.confirmOpen = false;
    if (!this.fine) return;

    const reason = this.reasonDraft.trim();
    this.processing = true;
    this.actionError = '';

    this.fineService.cancel(this.fine.id, reason).subscribe({
      next: () => {
        this.processing = false;
        this.reasonDraft = '';
        this.load();
      },
      error: () => {
        this.processing = false;
        this.actionError = 'Unable to cancel this fine.';
      }
    });
  }

  onDialogCancelled(): void {
    this.confirmOpen = false;
  }

  back(): void {
    this.router.navigate(['/admin/fines']);
  }
}
