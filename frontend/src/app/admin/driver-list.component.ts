import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { DriverService } from '../services/driver.service';
import { DriverDto, DriverSearchObject } from '../models/driver.model';
import { DataTableColumn } from '../shared/data-table/data-table.component';

const PAGE_SIZE = 20;

/**
 * Admin driver directory — GET /api/drivers (search) only. No delete action:
 * DriverService (confirmed) exposes no delete/deactivate/decommission method
 * of any name, unlike UserService.deleteUser or CameraService.decommission,
 * so there's no confirmed way to remove a driver record via the API. Scope
 * otherwise kept to basic CRUD (list + create/edit) on purpose —
 * suspend/liftSuspension/linkUserAccount exist on DriverService but are left
 * for a later pass and aren't wired into this list.
 */
@Component({
  selector: 'app-driver-list',
  standalone: false,
  templateUrl: './driver-list.component.html'
})
export class DriverListComponent implements OnInit {
  rows: DriverDto[] = [];
  loading = false;
  error = '';

  page = 0;
  hasMore = false;
  count = 0;

  search = '';
  suspendedFilter: '' | 'true' | 'false' = '';
  expiredFilter: '' | 'true' | 'false' = '';

  columns: DataTableColumn<DriverDto>[] = [
    { key: 'licenceNumber', label: 'Licence #', mono: true },
    { key: 'nationalId', label: 'National ID', mono: true },
    { key: 'firstName', label: 'Name', format: (row) => `${row.firstName} ${row.lastName}` },
    { key: 'email', label: 'Email' },
    { key: 'phoneNumber', label: 'Phone' },
    { key: 'licenseCategory', label: 'Category', align: 'center' },
    { key: 'penaltyPoints', label: 'Points', align: 'center' },
    { key: 'isSuspended', label: 'Status', align: 'center', format: (row) => (row.isSuspended ? 'Suspended' : 'Active') },
    {
      key: 'licenceExpiresAt',
      label: 'Licence expires',
      format: (row) => (row.licenceExpiresAt ? new Date(row.licenceExpiresAt).toLocaleDateString() : '—')
    }
  ];

  constructor(private driverService: DriverService, private router: Router) {}

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading = true;
    this.error = '';

    const searchObject: DriverSearchObject = {
      page: this.page,
      limit: PAGE_SIZE,
      includeCount: true,
      search: this.search.trim() || undefined,
      isSuspended: this.suspendedFilter === '' ? undefined : this.suspendedFilter === 'true',
      licenseExpired: this.expiredFilter === '' ? undefined : this.expiredFilter === 'true'
    };

    this.driverService.search(searchObject).subscribe({
      next: (result) => {
        this.rows = result?.resultList ?? [];
        this.count = result?.count ?? 0;
        this.hasMore = result?.hasMore ?? false;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load drivers.';
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
    this.router.navigate(['/admin/drivers/new']);
  }

  edit(driver: DriverDto): void {
    this.router.navigate(['/admin/drivers', driver.id, 'edit']);
  }
}