import { Component, Input } from '@angular/core';

/**
 * Colored status pill. Covers every status enum in the system (violation,
 * fine, appeal, payment, report, job) — the color mapping keys off the
 * literal string value, so it works for any of them without configuration.
 * See models/enums.ts for the canonical spelling of each value, including
 * the confirmed-real ones like DISUPTED/DISSMISED.
 */
@Component({
  selector: 'app-status-badge',
  standalone: false,
  template: `<span class="status-badge" [attr.data-status]="status">{{ status }}</span>`,
  styleUrls: ['./status-badge.component.css']
})
export class StatusBadgeComponent {
  @Input() status: string | null | undefined = '';
}
