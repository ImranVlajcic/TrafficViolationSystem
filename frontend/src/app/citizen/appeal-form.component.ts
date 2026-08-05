import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { AppealService } from '../services/appeal.service';
import { AppealCreateRequest, AppealDto } from '../models/appeal.model';

/**
 * New appeal form, reachable as /citizen/appeals/new/:violationId (linked
 * from a future violation record) or bare /citizen/appeals/new. Without a
 * violations list built yet there's nowhere for a citizen to pick a
 * violation from, so when there's no route param the violationId field
 * falls back to manual entry instead of blocking the form entirely.
 */
@Component({
  selector: 'app-appeal-form',
  standalone: false,
  templateUrl: './appeal-form.component.html',
  styleUrls: ['./appeal-form.component.css']
})
export class AppealFormComponent implements OnInit {
  violationId = '';
  violationIdLocked = false;

  reason = '';
  evidenceUrl = '';

  submitting = false;
  submitError = '';
  result: AppealDto | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private appealService: AppealService
  ) {}

  ngOnInit(): void {
    const routeViolationId = this.route.snapshot.paramMap.get('violationId');
    if (routeViolationId) {
      this.violationId = routeViolationId;
      this.violationIdLocked = true;
    }
  }

  get canSubmit(): boolean {
    return !!this.violationId.trim() && !!this.reason.trim() && !this.submitting;
  }

  submit(): void {
    if (!this.canSubmit) return;

    this.submitting = true;
    this.submitError = '';

    const request: AppealCreateRequest = {
      violationId: this.violationId.trim(),
      reason: this.reason.trim(),
      evidenceUrl: this.evidenceUrl.trim() || undefined
    };

    this.appealService.create(request).subscribe({
      next: (appeal) => {
        this.submitting = false;
        this.result = appeal;
      },
      error: (err) => {
        this.submitting = false;
        this.submitError = err?.error?.message ?? 'Unable to submit your appeal.';
      }
    });
  }

  goToAppeals(): void {
    this.router.navigate(['/citizen/appeals']);
  }
}