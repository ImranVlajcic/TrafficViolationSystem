import { Component, EventEmitter, Input, Output } from '@angular/core';

export type ConfirmDialogTone = 'default' | 'danger';

/**
 * Presentational confirm dialog. Each consumer owns its own `open` boolean
 * and passes copy in — no global service/singleton, so state stays local
 * and easy to reason about:
 *
 *   <app-confirm-dialog
 *     [open]="showCancelConfirm"
 *     tone="danger"
 *     title="Cancel this fine?"
 *     message="This can't be undone."
 *     confirmLabel="Cancel fine"
 *     (confirmed)="cancelFine()"
 *     (cancelled)="showCancelConfirm = false"
 *   ></app-confirm-dialog>
 */
@Component({
  selector: 'app-confirm-dialog',
  standalone: false,
  templateUrl: './confirm-dialog.component.html',
  styleUrls: ['./confirm-dialog.component.css']
})
export class ConfirmDialogComponent {
  @Input() open = false;
  @Input() tone: ConfirmDialogTone = 'default';
  @Input() title = 'Are you sure?';
  @Input() message = '';
  @Input() confirmLabel = 'Confirm';
  @Input() cancelLabel = 'Cancel';

  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  onConfirm(): void {
    this.confirmed.emit();
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
