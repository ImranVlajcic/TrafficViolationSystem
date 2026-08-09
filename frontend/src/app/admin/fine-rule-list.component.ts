import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { FineRuleService } from '../services/fine-rule.service';
import { FineRuleDto, FineRuleSearchObject, FineRuleUpdateRequest } from '../models/finerule.model';
import { ViolationType } from '../models/enums';
import { DataTableColumn } from '../shared/data-table/data-table.component';

const VIOLATION_TYPE_OPTIONS: { value: ViolationType; label: string }[] = [
  { value: 'SPEEDING', label: 'Speeding' },
  { value: 'RED_LIGHT', label: 'Red light' },
  { value: 'NO_SEATBELT', label: 'No seatbelt' },
  { value: 'PHONE_USE', label: 'Phone use' },
  { value: 'WRONG_WAY', label: 'Wrong way' },
  { value: 'PARKING', label: 'Parking' },
  { value: 'DUI', label: 'DUI' },
  { value: 'NO_INSURANCE', label: 'No insurance' },
  { value: 'OVERLOAD', label: 'Overload' },
  { value: 'ILLEGAL_OVERTAKE', label: 'Illegal overtake' },
  { value: 'WRONG_LANE', label: 'Wrong lane' },
  { value: 'PEDESTRIAN_CROSSING', label: 'Pedestrian crossing' },
  { value: 'EXPIRED_REGISTRATION', label: 'Expired registration' },
  { value: 'OTHER', label: 'Other' }
];

const PAGE_SIZE = 20;

/**
 * Admin fine rules — GET /api/fine-rules (search). No delete endpoint —
 * per FineRuleService's own comment, rules are deactivated via update
 * (isActive: false) rather than removed, so "Deactivate"/"Reactivate"
 * both go through update(). Deactivate is gated by app-confirm-dialog
 * (destructive-ish: stops the rule applying to new violations);
 * Reactivate isn't (non-destructive, immediately reversible).
 *
 * FineRuleSearchObject only exposes violationType/isActive — no free-text
 * `search` field — so there's no search box here, just the two selects.
 */
@Component({
  selector: 'app-fine-rule-list',
  standalone: false,
  templateUrl: './fine-rule-list.component.html'
})
export class FineRuleListComponent implements OnInit {
  rows: FineRuleDto[] = [];
  loading = false;
  error = '';

  page = 0;
  hasMore = false;
  count = 0;

  typeFilter: ViolationType | '' = '';
  activeFilter: '' | 'true' | 'false' = '';

  typeOptions = VIOLATION_TYPE_OPTIONS;

  deactivateTarget: FineRuleDto | null = null;
  deactivating = false;
  actionError = '';

  columns: DataTableColumn<FineRuleDto>[] = [
    { key: 'violationType', label: 'Violation type' },
    { key: 'baseAmount', label: 'Base', align: 'right', mono: true },
    { key: 'minAmount', label: 'Min', align: 'right', mono: true },
    { key: 'maxAmount', label: 'Max', align: 'right', mono: true },
    { key: 'penaltyPoints', label: 'Points', align: 'center' },
    { key: 'paymentDueDates', label: 'Due (days)', align: 'center' },
    {
      key: 'earlyPayDiscountPct',
      label: 'Early pay %',
      align: 'right',
      format: (row) => (row.earlyPayDiscountPct != null ? `${row.earlyPayDiscountPct}%` : '—')
    },
    {
      key: 'lateSurchargePct',
      label: 'Late surcharge %',
      align: 'right',
      format: (row) => (row.lateSurchargePct != null ? `${row.lateSurchargePct}%` : '—')
    },
    { key: 'isActive', label: 'Status', align: 'center', format: (row) => (row.isActive ? 'Active' : 'Inactive') }
  ];

  constructor(private fineRuleService: FineRuleService, private router: Router) {}

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading = true;
    this.error = '';

    const searchObject: FineRuleSearchObject = {
      page: this.page,
      limit: PAGE_SIZE,
      includeCount: true,
      violationType: this.typeFilter || undefined,
      isActive: this.activeFilter === '' ? undefined : this.activeFilter === 'true'
    };

    this.fineRuleService.search(searchObject).subscribe({
      next: (result) => {
        this.rows = result?.resultList ?? [];
        this.count = result?.count ?? 0;
        this.hasMore = result?.hasMore ?? false;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load fine rules.';
        this.loading = false;
      }
    });
  }

  onFilterSubmit(): void {
    this.page = 0;
    this.load();
  }

  onPageChange(page: number): void {
    this.page = page;
    this.load();
  }

  createNew(): void {
    this.router.navigate(['/admin/fine-rules/new']);
  }

  edit(rule: FineRuleDto): void {
    this.router.navigate(['/admin/fine-rules', rule.id, 'edit']);
  }

  confirmDeactivate(rule: FineRuleDto): void {
    this.actionError = '';
    this.deactivateTarget = rule;
  }

  cancelDeactivate(): void {
    this.deactivateTarget = null;
  }

  get deactivateMessage(): string {
    return this.deactivateTarget
      ? `Deactivate the ${this.deactivateTarget.violationType.toLowerCase().replace(/_/g, ' ')} rule? It will stop applying to new violations.`
      : '';
  }

  doDeactivate(): void {
    if (!this.deactivateTarget) return;

    this.deactivating = true;
    this.actionError = '';

    this.fineRuleService.update(this.deactivateTarget.id, { isActive: false }).subscribe({
      next: () => {
        this.deactivating = false;
        this.deactivateTarget = null;
        this.load();
      },
      error: () => {
        this.deactivating = false;
        this.actionError = 'Unable to deactivate this rule.';
      }
    });
  }

  reactivate(rule: FineRuleDto): void {
    this.actionError = '';
    const request: FineRuleUpdateRequest = { isActive: true };

    this.fineRuleService.update(rule.id, request).subscribe({
      next: () => this.load(),
      error: () => {
        this.actionError = 'Unable to reactivate this rule.';
      }
    });
  }
}
