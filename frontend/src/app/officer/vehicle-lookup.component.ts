import { Component } from '@angular/core';

// Adjust these import paths to match your actual project layout.
import { VehicleService } from '../services/vehicle.service';
import { VehicleDto } from '../models/vehicle.model';
import { DataTableColumn } from '../shared/data-table/data-table.component';

/**
 * Officer "quick lookup" by plate or VIN.
 *
 * NOT CONFIRMED — flagging per the task's own caveat: VehicleSearchObject
 * has no dedicated licencePlate/vin filter fields, only a generic
 * `search` free-text field. This assumes the backend matches it against
 * both plate and VIN server-side (same assumption as driver-lookup).
 * Unlike DriverService, VehicleService has no dedicated exact-match
 * lookup to fall back on — verify against VehicleController.java before
 * relying on this.
 */
@Component({
  selector: 'app-vehicle-lookup',
  standalone: false,
  templateUrl: './vehicle-lookup.component.html'
})
export class VehicleLookupComponent {
  query = '';
  results: VehicleDto[] = [];
  searched = false;
  loading = false;
  error = '';

  columns: DataTableColumn<VehicleDto>[] = [
    { key: 'licencePlate', label: 'Plate', mono: true },
    { key: 'vin', label: 'VIN', mono: true },
    { key: 'make', label: 'Make' },
    { key: 'model', label: 'Model' },
    { key: 'year', label: 'Year', align: 'center' },
    {
      key: 'isStolen',
      label: 'Status',
      align: 'center',
      format: (row) => (row.isStolen ? 'Stolen' : row.isActive ? 'Active' : 'Inactive')
    }
  ];

  constructor(private vehicleService: VehicleService) {}

  search(): void {
    const search = this.query.trim();
    if (!search) {
      return;
    }

    this.loading = true;
    this.error = '';
    this.searched = true;

    this.vehicleService.search({ search, limit: 20 }).subscribe({
      next: (result) => {
        this.results = result?.resultList ?? [];
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to search vehicles.';
        this.loading = false;
      }
    });
  }
}
