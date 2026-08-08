import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { VehicleService } from '../services/vehicle.service';
import { VehicleDto, VehicleSearchObject } from '../models/vehicle.model';
import { VehicleType } from '../models/enums';
import { DataTableColumn } from '../shared/data-table/data-table.component';

const VEHICLE_TYPE_OPTIONS: { value: VehicleType; label: string }[] = [
  { value: 'CAR', label: 'Car' },
  { value: 'MOTORCYCLE', label: 'Motorcycle' },
  { value: 'VAN', label: 'Van' },
  { value: 'TRUCK', label: 'Truck' },
  { value: 'BUS', label: 'Bus' },
  { value: 'TRACTOR', label: 'Tractor' },
  { value: 'OTHER', label: 'Other' }
];

const PAGE_SIZE = 20;

/**
 * Admin vehicle directory — GET /api/vehicles (search),
 * DELETE /api/vehicles/{id} (deregister), POST .../mark-stolen,
 * POST .../mark-found. Transfer ownership routes to a dedicated form
 * (vehicle-transfer) rather than an inline dialog, since it needs
 * newOwnerId/transferDate/notes fields the confirm dialog has no slot for
 * — same reasoning as camera-maintenance being its own routed page.
 *
 * NOTE: unlike users/drivers (page 1-based), this project's vehicle
 * pagination is confirmed 0-indexed — `page` starts at 0 here, not 1.
 */
@Component({
  selector: 'app-vehicle-list',
  standalone: false,
  templateUrl: './vehicle-list.component.html'
})
export class VehicleListComponentAdmin implements OnInit {
  rows: VehicleDto[] = [];
  loading = false;
  error = '';

  page = 0;
  hasMore = false;
  count = 0;

  search = '';
  vehicleTypeFilter: VehicleType | '' = '';
  stolenFilter: '' | 'true' | 'false' = '';
  activeFilter: '' | 'true' | 'false' = '';

  vehicleTypeOptions = VEHICLE_TYPE_OPTIONS;

  actionError = '';

  deregisterTarget: VehicleDto | null = null;
  deregistering = false;

  stolenActionTarget: { vehicle: VehicleDto; markingStolen: boolean } | null = null;
  updatingStolen = false;

  columns: DataTableColumn<VehicleDto>[] = [
    { key: 'licencePlate', label: 'Plate', mono: true },
    { key: 'vin', label: 'VIN', mono: true },
    { key: 'make', label: 'Vehicle', format: (row) => `${row.make} ${row.model} (${row.year})` },
    { key: 'color', label: 'Color' },
    { key: 'vehicleType', label: 'Type', align: 'center' },
    { key: 'fuelType', label: 'Fuel', align: 'center' },
    { key: 'ownerId', label: 'Owner', mono: true },
    {
      key: 'registrationExpiry',
      label: 'Registration expires',
      format: (row) => (row.registrationExpiry ? new Date(row.registrationExpiry).toLocaleDateString() : '—')
    },
    {
      key: 'isStolen',
      label: 'Status',
      align: 'center',
      format: (row) => (row.isStolen ? 'Stolen' : row.isActive ? 'Active' : 'Inactive')
    }
  ];

  constructor(private vehicleService: VehicleService, private router: Router) {}

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading = true;
    this.error = '';

    const searchObject: VehicleSearchObject = {
      page: this.page,
      limit: PAGE_SIZE,
      includeCount: true,
      search: this.search.trim() || undefined,
      vehicleType: this.vehicleTypeFilter || undefined,
      isStolen: this.stolenFilter === '' ? undefined : this.stolenFilter === 'true',
      isActive: this.activeFilter === '' ? undefined : this.activeFilter === 'true'
    };

    this.vehicleService.search(searchObject).subscribe({
      next: (result) => {
        this.rows = result?.resultList ?? [];
        this.count = result?.count ?? 0;
        this.hasMore = result?.hasMore ?? false;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load vehicles.';
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
    this.router.navigate(['/admin/vehicles/new']);
  }

  edit(vehicle: VehicleDto): void {
    this.router.navigate(['/admin/vehicles', vehicle.id, 'edit']);
  }

  transferOwnership(vehicle: VehicleDto): void {
    this.router.navigate(['/admin/vehicles', vehicle.id, 'transfer-ownership']);
  }

  confirmDeregister(vehicle: VehicleDto): void {
    this.actionError = '';
    this.deregisterTarget = vehicle;
  }

  cancelDeregister(): void {
    this.deregisterTarget = null;
  }

  get deregisterMessage(): string {
    return this.deregisterTarget
      ? `Deregister ${this.deregisterTarget.licencePlate} (${this.deregisterTarget.make} ${this.deregisterTarget.model})? This can be reversed later by another admin.`
      : '';
  }

  doDeregister(): void {
    if (!this.deregisterTarget) return;

    this.deregistering = true;
    this.actionError = '';

    this.vehicleService.deregister(this.deregisterTarget.id).subscribe({
      next: () => {
        this.deregistering = false;
        this.deregisterTarget = null;
        this.load();
      },
      error: () => {
        this.deregistering = false;
        this.actionError = 'Unable to deregister this vehicle.';
      }
    });
  }

  confirmStolenAction(vehicle: VehicleDto): void {
    this.actionError = '';
    this.stolenActionTarget = { vehicle, markingStolen: !vehicle.isStolen };
  }

  cancelStolenAction(): void {
    this.stolenActionTarget = null;
  }

  get stolenActionTitle(): string {
    if (!this.stolenActionTarget) return '';
    return this.stolenActionTarget.markingStolen ? 'Mark as stolen?' : 'Mark as found?';
  }

  get stolenActionMessage(): string {
    if (!this.stolenActionTarget) return '';
    const v = this.stolenActionTarget.vehicle;
    return this.stolenActionTarget.markingStolen
      ? `Mark ${v.licencePlate} (${v.make} ${v.model}) as stolen?`
      : `Mark ${v.licencePlate} (${v.make} ${v.model}) as found, clearing the stolen flag?`;
  }

  doStolenAction(): void {
    if (!this.stolenActionTarget) return;

    const { vehicle, markingStolen } = this.stolenActionTarget;
    this.updatingStolen = true;
    this.actionError = '';

    const request$ = markingStolen
      ? this.vehicleService.markStolen(vehicle.id)
      : this.vehicleService.markFound(vehicle.id);

    request$.subscribe({
      next: () => {
        this.updatingStolen = false;
        this.stolenActionTarget = null;
        this.load();
      },
      error: () => {
        this.updatingStolen = false;
        this.actionError = markingStolen
          ? 'Unable to mark this vehicle as stolen.'
          : 'Unable to mark this vehicle as found.';
      }
    });
  }
}
