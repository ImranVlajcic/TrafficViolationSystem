import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { AppealService } from '../services/appeal.service';
import { UserService } from '../services/user.service';
import { AppealDto } from '../models/appeal.model';
import { AppealStatus } from '../models/enums';
import { DataTableColumn } from '../shared/data-table/data-table.component';

const STATUS_LABELS: Record<AppealStatus, string> = {
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  WITHDRAWN: 'Withdrawn'
};

/**
 * Citizen's own appeals.
 *
 * There's no GET /api/appeals/my — the only citizen-relevant scoping
 * route is GET /api/appeals/driver/{driverId}. Per confirmation that a
 * citizen's user record and driver record share the same id, this loads
 * the current user's own id via GET /api/users/me first, then uses that
 * as the driverId. If that 1:1 assumption ever stops holding, this is
 * the one place that needs to change.
 */
@Component({
  selector: 'app-appeal-list',
  standalone: false,
  templateUrl: './appeal-list.component.html',
  styleUrls: ['./appeal-list.component.css']
})
export class AppealListComponent implements OnInit {
  appeals: AppealDto[] = [];
  loading = false;
  error = '';

  withdrawingId: string | null = null;
  withdrawError = '';

  columns: DataTableColumn<AppealDto>[] = [
    { key: 'appealNumber', label: 'Appeal #', mono: true },
    {
      key: 'submittedAt',
      label: 'Submitted',
      format: (row) => new Date(row.submittedAt).toLocaleDateString()
    },
    { key: 'reason', label: 'Reason', format: (row) => this.truncate(row.reason, 60) },
    {
      key: 'status',
      label: 'Status',
      align: 'center',
      format: (row) => STATUS_LABELS[row.status] ?? row.status
    }
  ];

  constructor(
    private appealService: AppealService,
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';

    this.userService.getProfile().subscribe({
      next: (profileResponse) => {
        const driverId = profileResponse.data.id;
        this.appealService.getForDriver(driverId).subscribe({
          next: (response) => {
            this.appeals = response.data ?? [];
            this.loading = false;
          },
          error: () => {
            this.error = 'Unable to load your appeals.';
            this.loading = false;
          }
        });
      },
      error: () => {
        this.error = 'Unable to load your profile.';
        this.loading = false;
      }
    });
  }

  newAppeal(): void {
    this.router.navigate(['/citizen/appeals/new']);
  }

  canWithdraw(row: AppealDto): boolean {
    return row.status === 'SUBMITTED';
  }

  withdraw(row: AppealDto): void {
    this.withdrawError = '';
    this.withdrawingId = row.id;

    this.appealService.withdraw(row.id).subscribe({
      next: () => {
        this.withdrawingId = null;
        this.load();
      },
      error: () => {
        this.withdrawingId = null;
        this.withdrawError = 'Unable to withdraw this appeal.';
      }
    });
  }

  private truncate(value: string, max: number): string {
    return value.length > max ? `${value.slice(0, max - 1)}…` : value;
  }
}