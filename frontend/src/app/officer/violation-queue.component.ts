import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { ViolationService } from '../services/violation.service';
import { ViolationDto } from '../models/violation.model';
import { DataTableColumn } from '../shared/data-table/data-table.component';
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
 * Officer's review queue — GET /api/violations/pending — with Confirm/
 * Dismiss row actions. Both confirm() and dismiss() take a required
 * ReviewViolationRequest.reviewNotes, but ConfirmDialogComponent has no
 * slot for custom form fields (just title/message/actions), so it's used
 * purely as the "are you sure?" gate — each row carries its own inline
 * notes input, and the buttons are disabled until notes are entered.
 */
@Component({
  selector: 'app-violation-queue',
  standalone: false,
  templateUrl: './violation-queue.component.html',
  styleUrls: ['./violation-queue.component.css']
})
export class ViolationQueueComponent implements OnInit {
  violations: ViolationDto[] = [];
  loading = false;
  error = '';

  notesDraft: Record<string, string> = {};
  actionError = '';
  processingId: string | null = null;

  confirmOpen = false;
  confirmTone: ConfirmDialogTone = 'default';
  confirmTitle = '';
  confirmMessage = '';
  private pendingRow: ViolationDto | null = null;
  private pendingAction: ReviewAction | null = null;

  columns: DataTableColumn<ViolationDto>[] = [
    { key: 'referenceNumber', label: 'Reference', mono: true },
    { key: 'violationType', label: 'Type', format: (row) => humanize(row.violationType) },
    { key: 'occuredAt', label: 'Occurred', format: (row) => new Date(row.occuredAt).toLocaleString() },
    { key: 'detectionMethod', label: 'Detection', format: (row) => humanize(row.detectionMethod) },
    { key: 'locationDescription', label: 'Location', format: (row) => row.locationDescription ?? '—' }
  ];

  constructor(
    private violationService: ViolationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';

    this.violationService.getPendingReview().subscribe({
      next: (response) => {
        this.violations = response.data ?? [];
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load the review queue.';
        this.loading = false;
      }
    });
  }

  viewViolation(row: ViolationDto): void {
    this.router.navigate(['/officer/violations', row.id]);
  }

  canSubmit(row: ViolationDto): boolean {
    return !!this.notesDraft[row.id]?.trim();
  }

  requestConfirm(row: ViolationDto): void {
    this.openDialog(row, 'confirm');
  }

  requestDismiss(row: ViolationDto): void {
    this.openDialog(row, 'dismiss');
  }

  private openDialog(row: ViolationDto, action: ReviewAction): void {
    this.actionError = '';
    this.pendingRow = row;
    this.pendingAction = action;
    this.confirmTone = action === 'dismiss' ? 'danger' : 'default';
    this.confirmTitle = action === 'dismiss' ? 'Dismiss this violation?' : 'Confirm this violation?';
    this.confirmMessage =
      action === 'dismiss'
        ? `${row.referenceNumber} will be dismissed. No fine will be issued.`
        : `${row.referenceNumber} will be confirmed and a fine will be issued.`;
    this.confirmOpen = true;
  }

  onDialogConfirmed(): void {
    this.confirmOpen = false;
    if (!this.pendingRow || !this.pendingAction) {
      return;
    }

    const row = this.pendingRow;
    const action = this.pendingAction;
    const reviewNotes = this.notesDraft[row.id]?.trim() ?? '';

    this.processingId = row.id;
    this.actionError = '';

    const request$ =
      action === 'confirm'
        ? this.violationService.confirm(row.id, { reviewNotes })
        : this.violationService.dismiss(row.id, { reviewNotes });

    request$.subscribe({
      next: () => {
        this.processingId = null;
        delete this.notesDraft[row.id];
        this.load();
      },
      error: () => {
        this.processingId = null;
        this.actionError = `Unable to ${action} this violation.`;
      }
    });
  }

  onDialogCancelled(): void {
    this.confirmOpen = false;
    this.pendingRow = null;
    this.pendingAction = null;
  }
}
