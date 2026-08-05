import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { ViolationService } from '../services/violation.service';
import { ViolationDto } from '../models/violation.model';
import { ConfirmDialogTone } from '../shared/confirm-dialog/confirm-dialog.component';

type ReviewAction = 'confirm' | 'dismiss';

function humanize(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

/**
 * Full violation record — GET /api/violations/{id}, inherited from
 * ViolationService's BaseCrudService (not explicitly listed in
 * violation.service.ts, but findById/search are assumed standard on the
 * base class the same way FineService/PaymentService define them
 * explicitly since they don't extend it — verify against BaseCrudService
 * if this doesn't compile).
 *
 * Confirm/Dismiss only make sense while still PENDING, so the review
 * form is hidden once the violation has moved to any other status.
 */
@Component({
  selector: 'app-violation-detail',
  standalone: false,
  templateUrl: './violation-detail.component.html'
})
export class ViolationDetailComponent implements OnInit {
  violation: ViolationDto | null = null;
  loading = false;
  error = '';

  reviewNotes = '';
  actionError = '';
  processing = false;

  confirmOpen = false;
  confirmTone: ConfirmDialogTone = 'default';
  confirmTitle = '';
  confirmMessage = '';
  private pendingAction: ReviewAction | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private violationService: ViolationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.load(id);
    }
  }

  load(id: string): void {
    this.loading = true;
    this.error = '';

    this.violationService.findById(id).subscribe({
      next: (violation) => {
        this.violation = violation;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load this violation.';
        this.loading = false;
      }
    });
  }

  humanize(value: string): string {
    return humanize(value);
  }

  formatDate(value?: string): string {
    return value ? new Date(value).toLocaleString() : '—';
  }

  get isPending(): boolean {
    return this.violation?.status === 'PENDING';
  }

  get canSubmit(): boolean {
    return !!this.reviewNotes.trim();
  }

  requestConfirm(): void {
    this.openDialog('confirm');
  }

  requestDismiss(): void {
    this.openDialog('dismiss');
  }

  private openDialog(action: ReviewAction): void {
    if (!this.violation) {
      return;
    }
    this.actionError = '';
    this.pendingAction = action;
    this.confirmTone = action === 'dismiss' ? 'danger' : 'default';
    this.confirmTitle = action === 'dismiss' ? 'Dismiss this violation?' : 'Confirm this violation?';
    this.confirmMessage =
      action === 'dismiss'
        ? `${this.violation.referenceNumber} will be dismissed. No fine will be issued.`
        : `${this.violation.referenceNumber} will be confirmed and a fine will be issued.`;
    this.confirmOpen = true;
  }

  onDialogConfirmed(): void {
    this.confirmOpen = false;
    if (!this.violation || !this.pendingAction) {
      return;
    }

    const action = this.pendingAction;
    const reviewNotes = this.reviewNotes.trim();
    const id = this.violation.id;

    this.processing = true;
    this.actionError = '';

    const request$ =
      action === 'confirm'
        ? this.violationService.confirm(id, { reviewNotes })
        : this.violationService.dismiss(id, { reviewNotes });

    request$.subscribe({
      next: (response) => {
        this.processing = false;
        this.violation = response.data ?? this.violation;
        this.reviewNotes = '';
      },
      error: () => {
        this.processing = false;
        this.actionError = `Unable to ${action} this violation.`;
      }
    });
  }

  onDialogCancelled(): void {
    this.confirmOpen = false;
    this.pendingAction = null;
  }

  back(): void {
    this.router.navigate(['/officer/violations']);
  }
}
