import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { ViolationService } from '../services/violation.service';
import { ViolationDto } from '../models/violation.model';
import { ViolationStatus } from '../models/enums';
import { DataTableColumn } from '../shared/data-table/data-table.component';

// NOTE: the enum value is DISSMISED (typo baked into ViolationStatus itself),
// not DISMISSED — the label text below is still spelled correctly since
// that's just what's shown to the user.
const STATUS_LABELS: Record<ViolationStatus, string> = {
  PENDING: 'Pending review',
  CONFIRMED: 'Confirmed',
  DISSMISED: 'Dismissed',
  DISUPTED: 'Disputed',
  CLOSED: 'Closed'
};

/**
 * Citizen's own violations — GET /api/violations/my.
 */
@Component({
  selector: 'app-violation-list',
  standalone: false,
  templateUrl: './violation-list.component.html',
  styleUrls: ['./violation-list.component.css']
})
export class ViolationListComponent implements OnInit {
  violations: ViolationDto[] = [];
  loading = false;
  error = '';

  columns: DataTableColumn<ViolationDto>[] = [
    { key: 'referenceNumber', label: 'Reference #', mono: true },
    { key: 'violationType', label: 'Type' },
    {
      key: 'occuredAt',
      label: 'Date',
      format: (row) => new Date(row.occuredAt).toLocaleDateString()
    },
    {
      key: 'status',
      label: 'Status',
      align: 'center',
      format: (row) => STATUS_LABELS[row.status] ?? row.status
    }
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

    this.violationService.getMyViolations().subscribe({
      next: (response) => {
        this.violations = response.data ?? [];
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load your violations.';
        this.loading = false;
      }
    });
  }

  /** Confirmed violations are the ones that can still be appealed. */
  canAppeal(row: ViolationDto): boolean {
    return row.status === 'CONFIRMED';
  }

  fileAppeal(row: ViolationDto): void {
    this.router.navigate(['/citizen/appeals/new', row.id]);
  }
}