import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { FineRuleService } from '../services/fine-rule.service';
import { FineRuleDto, FineRuleCreateRequest, FineRuleUpdateRequest } from '../models/finerule.model';
import { ViolationType } from '../models/enums';

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

/**
 * Admin create/edit fine rule, reached via /admin/fine-rules/new and
 * /admin/fine-rules/:id/edit.
 *
 * violationType is create-only/disabled-in-edit — FineRuleUpdateRequest
 * has no violationType field, so a rule's violation type is fixed at
 * creation (makes sense: it's the rule's identity/lookup key, not an
 * editable attribute). isActive is edit-only, same pattern as camera/zone
 * forms — a new rule can't be created inactive.
 *
 * Field names follow finerule.model.ts's entity/create-request naming
 * (paymentDueDates, earlyPayWindowDay) rather than the update-request
 * doc section's drifted spelling — see that file's note.
 */
@Component({
  selector: 'app-fine-rule-form',
  standalone: false,
  templateUrl: './fine-rule-form.component.html',
  styleUrls: ['./fine-rule-form.component.css']
})
export class FineRuleFormComponent implements OnInit {
  mode: 'create' | 'edit' = 'create';
  ruleId!: number;

  loading = false;
  loadError = '';
  submitting = false;
  submitError = '';

  typeOptions = VIOLATION_TYPE_OPTIONS;

  violationType: ViolationType = 'SPEEDING';
  baseAmount: number | null = null;
  minAmount: number | null = null;
  maxAmount: number | null = null;
  penaltyPoints: number | null = null;
  paymentDueDates: number | null = null;
  earlyPayDiscountPct: number | null = null;
  earlyPayWindowDay: number | null = null;
  lateSurchargePct: number | null = null;
  description = '';
  isActive = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fineRuleService: FineRuleService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.mode = 'edit';
      this.ruleId = Number(idParam);
      this.loadRule(this.ruleId);
    }
  }

  private loadRule(id: number): void {
    this.loading = true;
    this.loadError = '';

    this.fineRuleService.findById(id).subscribe({
      next: (rule: FineRuleDto) => {
        this.violationType = rule.violationType;
        this.baseAmount = rule.baseAmount;
        this.minAmount = rule.minAmount;
        this.maxAmount = rule.maxAmount;
        this.penaltyPoints = rule.penaltyPoints;
        this.paymentDueDates = rule.paymentDueDates;
        this.earlyPayDiscountPct = rule.earlyPayDiscountPct ?? null;
        this.earlyPayWindowDay = rule.earlyPayWindowDay ?? null;
        this.lateSurchargePct = rule.lateSurchargePct ?? null;
        this.description = rule.description ?? '';
        this.isActive = rule.isActive;
        this.loading = false;
      },
      error: () => {
        this.loadError = 'Unable to load this fine rule.';
        this.loading = false;
      }
    });
  }

  get canSubmit(): boolean {
    if (this.submitting || this.loading) return false;
    return (
      this.baseAmount !== null &&
      this.minAmount !== null &&
      this.maxAmount !== null &&
      this.penaltyPoints !== null &&
      this.paymentDueDates !== null
    );
  }

  submit(): void {
    if (this.mode === 'create') {
      this.submitCreate();
    } else {
      this.submitUpdate();
    }
  }

  private submitCreate(): void {
    const request: FineRuleCreateRequest = {
      violationType: this.violationType,
      baseAmount: this.baseAmount as number,
      minAmount: this.minAmount as number,
      maxAmount: this.maxAmount as number,
      penaltyPoints: this.penaltyPoints as number,
      paymentDueDates: this.paymentDueDates as number,
      earlyPayDiscountPct: this.earlyPayDiscountPct ?? undefined,
      earlyPayWindowDay: this.earlyPayWindowDay ?? undefined,
      lateSurchargePct: this.lateSurchargePct ?? undefined,
      description: this.description || undefined
    };

    this.submitting = true;
    this.submitError = '';

    this.fineRuleService.create(request).subscribe({
      next: () => {
        this.submitting = false;
        this.router.navigate(['/admin/fine-rules']);
      },
      error: (err) => {
        this.submitting = false;
        this.submitError = err?.error?.message ?? 'Unable to create this fine rule.';
      }
    });
  }

  private submitUpdate(): void {
    const request: FineRuleUpdateRequest = {
      baseAmount: this.baseAmount as number,
      minAmount: this.minAmount as number,
      maxAmount: this.maxAmount as number,
      penaltyPoints: this.penaltyPoints as number,
      paymentDueDates: this.paymentDueDates as number,
      earlyPayDiscountPct: this.earlyPayDiscountPct ?? undefined,
      earlyPayWindowDay: this.earlyPayWindowDay ?? undefined,
      lateSurchargePct: this.lateSurchargePct ?? undefined,
      description: this.description || undefined,
      isActive: this.isActive
    };

    this.submitting = true;
    this.submitError = '';

    this.fineRuleService.update(this.ruleId, request).subscribe({
      next: () => {
        this.submitting = false;
        this.router.navigate(['/admin/fine-rules']);
      },
      error: (err) => {
        this.submitting = false;
        this.submitError = err?.error?.message ?? 'Unable to update this fine rule.';
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/fine-rules']);
  }
}
