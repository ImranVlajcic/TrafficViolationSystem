import { Component } from '@angular/core';

// Adjust these import paths to match your actual project layout.
import { DriverService } from '../services/driver.service';
import { DriverDto } from '../models/driver.model';
import { DataTableColumn } from '../shared/data-table/data-table.component';

/**
 * Officer "quick lookup" by license number or national ID.
 *
 * NOT CONFIRMED — flagging per the task's own caveat: DriverSearchObject
 * only exposes a generic `search` free-text field, no dedicated
 * licenceNumber/nationalId filters. This assumes the backend matches
 * `search` against both fields server-side (mirroring how the `search`
 * field behaves on FineSearchObject/ViolationSearchObject/
 * VehicleSearchObject elsewhere in the docs) and uses DriverService's
 * inherited search() with that field.
 *
 * DriverService.findByLicenseNumber(licenseNumber) exists as an exact-
 * match alternative if that assumption is wrong — it only covers license
 * number, not national ID, and returns a single DriverDto rather than a
 * list, so it wasn't used as the primary lookup here. Verify both against
 * DriverController.java before relying on either.
 */
@Component({
  selector: 'app-driver-lookup',
  standalone: false,
  templateUrl: './driver-lookup.component.html'
})
export class DriverLookupComponent {
  query = '';
  results: DriverDto[] = [];
  searched = false;
  loading = false;
  error = '';

  columns: DataTableColumn<DriverDto>[] = [
    { key: 'licenceNumber', label: 'License #', mono: true },
    { key: 'nationalId', label: 'National ID', mono: true },
    { key: 'firstName', label: 'First name' },
    { key: 'lastName', label: 'Last name' },
    { key: 'penaltyPoints', label: 'Points', align: 'center' },
    {
      key: 'isSuspended',
      label: 'Status',
      align: 'center',
      format: (row) => (row.isSuspended ? 'Suspended' : 'Active')
    }
  ];

  constructor(private driverService: DriverService) {}

  search(): void {
    const search = this.query.trim();
    if (!search) {
      return;
    }

    this.loading = true;
    this.error = '';
    this.searched = true;

    this.driverService.search({ search, limit: 20 }).subscribe({
      next: (result) => {
        this.results = result?.resultList ?? [];
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to search drivers.';
        this.loading = false;
      }
    });
  }
}
