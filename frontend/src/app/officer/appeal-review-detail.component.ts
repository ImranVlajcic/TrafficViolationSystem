import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { AppealService } from '../services/appeal.service';
import { AppealDto, ReviewAppealRequest } from '../models/appeal.model';
import { ToastService } from '../core/toast.service';

type ReviewAction = 'approve' | 'reject';

/**
 * NOTE: ToastService.show()'s exact signature is assumed here
 * ({ message, tone }) based on toast.component.ts's usage of
 * `toast.tone` / `toast.message` — verify against the real
 * toast.service.ts and adjust if the method name/shape differs.
 */
@Component({
  selector: 'app-appeal-review-detail',
  standalone: false,
  templateUrl: './appeal-review-detail.component.html',
  styleUrls: ['./appeal-review-detail.component.css']
})
export class AppealReviewDetailComponent implements OnInit {
  appeal: AppealDto | null = null;
  loading = false;
  error = '';

  reviewNotes = '';
  starting = false;
  submitting = false;
  actionError = '';

  confirmOpen = false;
  pendingAction: ReviewAction | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private appealService: AppealService,
    private toastService: ToastService
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

    this.appealService.findById(this.id).subscribe({
      next: (appeal) => {
        this.appeal = appeal;
        this.reviewNotes = appeal.reviewNotes ?? '';
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load this appeal.';
        this.loading = false;
      }
    });
  }

  get canStartReview(): boolean {
    return this.appeal?.status === 'SUBMITTED';
  }

  get canDecide(): boolean {
    return this.appeal?.status === 'SUBMITTED' || this.appeal?.status === 'UNDER_REVIEW';
  }

  startReview(): void {
    if (!this.appeal) return;
    this.starting = true;
    this.actionError = '';

    this.appealService.startReview(this.appeal.id).subscribe({
      next: (response) => {
        this.appeal = response.data;
        this.starting = false;
        this.toastService.info('Appeal moved to under review.');
      },
      error: () => {
        this.starting = false;
        this.actionError = 'Unable to start review.';
      }
    });
  }

  requestAction(action: ReviewAction): void {
    if (!this.reviewNotes.trim()) {
      this.actionError = 'Review notes are required before approving or rejecting.';
      return;
    }
    this.actionError = '';
    this.pendingAction = action;
    this.confirmOpen = true;
  }

  get confirmTone(): 'default' | 'danger' {
    return this.pendingAction === 'reject' ? 'danger' : 'default';
  }

  get confirmTitle(): string {
    return this.pendingAction === 'approve' ? 'Approve this appeal?' : 'Reject this appeal?';
  }

  get confirmMessage(): string {
    return this.pendingAction === 'approve'
      ? 'The fine will be cancelled and points reversed for the driver.'
      : 'The fine will be reinstated as unpaid.';
  }

  confirmDecision(): void {
    if (!this.appeal || !this.pendingAction) return;

    const request: ReviewAppealRequest = { reviewNotes: this.reviewNotes.trim() };
    this.submitting = true;
    this.confirmOpen = false;

    const call$ =
      this.pendingAction === 'approve'
        ? this.appealService.approve(this.appeal.id, request)
        : this.appealService.reject(this.appeal.id, request);

    call$.subscribe({
      next: (response) => {
        this.appeal = response.data;
        this.submitting = false;
        if (this.pendingAction === 'approve') {
          this.toastService.success('Appeal approved.');
        } else {
          this.toastService.error('Appeal rejected.');
        }
        this.pendingAction = null;
      },
      error: () => {
        this.submitting = false;
        this.actionError = 'Unable to submit your decision.';
        this.pendingAction = null;
      }
    });
  }

  cancelDecision(): void {
    this.confirmOpen = false;
    this.pendingAction = null;
  }

  back(): void {
    this.router.navigate(['/officer/appeals']);
  }
}