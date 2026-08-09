import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { ViolationService } from '../services/violation.service';
import { ViolationDto, ViolationSearchObject } from '../models/violation.model';
import { ViolationStatus, ViolationType, DetectionMethod } from '../models/enums';
import { DataTableColumn } from '../shared/data-table/data-table.component';

const PAGE_SIZE = 20;

function humanize(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

/**
 * Admin-wide violations browser — GET /api/violations (search), same
 * BaseCrudService.search() shape as DriverService (resultList/count/
 * hasMore), NOT the officer queue's GET /api/violations/pending. Admin can
 * see every violation regardless of status. No confirm/dismiss here — that
 * stays an officer-only action; this is list + view only, matching the
 * driver-list precedent (no delete on ViolationService either — it's not a
 * BaseCrudService method confirmed here beyond search, and there's no
 * reason to delete a violation record).
 *
 * "View" routes to /admin/violations/:id, which does not exist yet — the
 * only violation-detail component in the project is the officer one, and
 * it has confirm/dismiss wired in, which admin shouldn't get for free. A
 * read-only admin detail view is still pending.
 *
 * Field names below are copied exactly from the real ViolationSearchObject/
 * ViolationDto, typos included (violaitonType, vehicleid, occuredAt) —
 * do not "fix" these locally, they have to match what the backend actually
 * accepts/returns.
 */
@Component({
  selector: 'app-admin-violation-list',
  standalone: false,
  templateUrl: './violation-list.component.html'
})
export class ViolationListComponent implements OnInit {
  rows: ViolationDto[] = [];
  loading = false;
  error = '';

  page = 0;
  hasMore = false;
  count = 0;

  search = '';
  statusFilter: '' | ViolationStatus = '';
  violationTypeFilter: '' | ViolationType = '';
  detectionMethodFilter: '' | DetectionMethod = '';

  // Values copied exactly from enums.ts. DISUPTED and DISSMISED are both
  // confirmed real, not typos — they're two distinct statuses, not one
  // misspelled the same way twice.
  readonly statusOptions: ViolationStatus[] = ['PENDING', 'CONFIRMED', 'DISUPTED', 'DISSMISED', 'CLOSED'];
  readonly violationTypeOptions: ViolationType[] = [
    'SPEEDING',
    'RED_LIGHT',
    'NO_SEATBELT',
    'PHONE_USE',
    'WRONG_WAY',
    'PARKING',
    'DUI',
    'NO_INSURANCE',
    'OVERLOAD',
    'ILLEGAL_OVERTAKE',
    'WRONG_LANE',
    'PEDESTRIAN_CROSSING',
    'EXPIRED_REGISTRATION',
    'OTHER'
  ];
  readonly detectionMethodOptions: DetectionMethod[] = ['CAMERA_AUTO', 'RADAR_AUTO', 'MANUAL_OFFICER'];

  columns: DataTableColumn<ViolationDto>[] = [
    { key: 'referenceNumber', label: 'Reference', mono: true },
    { key: 'status', label: 'Status', format: (row) => humanize(row.status) },
    { key: 'violationType', label: 'Type', format: (row) => humanize(row.violationType) },
    { key: 'occuredAt', label: 'Occurred', format: (row) => new Date(row.occuredAt).toLocaleString() },
    { key: 'detectionMethod', label: 'Detection', format: (row) => humanize(row.detectionMethod) },
    { key: 'locationDescription', label: 'Location', format: (row) => row.locationDescription ?? '—' },
    { key: 'isAutomatic', label: 'Auto', align: 'center', format: (row) => (row.isAutomatic ? 'Yes' : 'No') }
  ];

  constructor(private violationService: ViolationService, private router: Router) {}

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading = true;
    this.error = '';

    const searchObject: ViolationSearchObject = {
      page: this.page,
      limit: PAGE_SIZE,
      includeCount: true,
      search: this.search.trim() || undefined,
      status: this.statusFilter || undefined,
      violaitonType: this.violationTypeFilter || undefined,
      detectionMethod: this.detectionMethodFilter || undefined
    };

    this.violationService.search(searchObject).subscribe({
      next: (result) => {
        this.rows = result?.resultList ?? [];
        this.count = result?.count ?? 0;
        this.hasMore = result?.hasMore ?? false;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load violations.';
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

  humanize(value: string): string {
    return humanize(value);
  }

  view(violation: ViolationDto): void {
    this.router.navigate(['/admin/violations', violation.id]);
  }
}