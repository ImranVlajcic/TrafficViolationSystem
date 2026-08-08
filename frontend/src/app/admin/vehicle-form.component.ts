import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { VehicleService } from '../services/vehicle.service';
import { VehicleDto, VehicleCreateRequest, VehicleUpdateRequest } from '../models/vehicle.model';
import { VehicleType, FuelType } from '../models/enums';

const VEHICLE_TYPE_OPTIONS: { value: VehicleType; label: string }[] = [
  { value: 'CAR', label: 'Car' },
  { value: 'MOTORCYCLE', label: 'Motorcycle' },
  { value: 'VAN', label: 'Van' },
  { value: 'TRUCK', label: 'Truck' },
  { value: 'BUS', label: 'Bus' },
  { value: 'TRACTOR', label: 'Tractor' },
  { value: 'OTHER', label: 'Other' }
];

const FUEL_TYPE_OPTIONS: { value: FuelType; label: string }[] = [
  { value: 'GASOLINE', label: 'Gasoline' },
  { value: 'DIESEL', label: 'Diesel' },
  { value: 'ELECTRIC', label: 'Electric' },
  { value: 'HYBRID', label: 'Hybrid' },
  { value: 'LPG', label: 'LPG' },
  { value: 'CNG', label: 'CNG' },
  { value: 'HYDROGEN', label: 'Hydrogen' },
  { value: 'OTHER', label: 'Other' }
];

/**
 * Admin create/edit vehicle, reached via /admin/vehicles/new and
 * /admin/vehicles/:id/edit.
 *
 * licencePlate, vin, enginceCc (doc's literal — likely a typo for
 * engineCc, kept as-is per VehicleDto), and ownerId have no field on
 * VehicleUpdateRequest, so they're shown read-only in edit mode rather
 * than sent back to the server (same pattern as `licenceNumber` in
 * driver-form). isStolen is also not on VehicleUpdateRequest — shown as a
 * read-only status line; use the "Mark stolen"/"Mark found" actions on the
 * list instead. Ownership itself is changed via the separate
 * vehicle-transfer form, not this one.
 *
 * ASSUMPTION, NOT CONFIRMED: ownerId is entered as a raw UUID text field —
 * there's no citizen/user picker component available to wire in here.
 */
@Component({
  selector: 'app-vehicle-form',
  standalone: false,
  templateUrl: './vehicle-form.component.html',
  styleUrls: ['./vehicle-form.component.css']
})
export class VehicleFormComponent implements OnInit {
  mode: 'create' | 'edit' = 'create';
  vehicleId = '';

  loading = false;
  loadError = '';
  submitting = false;
  submitError = '';

  vehicleTypeOptions = VEHICLE_TYPE_OPTIONS;
  fuelTypeOptions = FUEL_TYPE_OPTIONS;

  licencePlate = '';
  vin = '';
  make = '';
  model = '';
  year: number | null = null;
  color = '';
  vehicleType: VehicleType = 'CAR';
  enginceCc: number | null = null;
  fuelType: FuelType = 'GASOLINE';
  registrationDate = '';
  registrationExpiry = '';
  ownerId = '';
  isActive = true;

  // Display-only in edit mode — not part of VehicleUpdateRequest.
  isStolen = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private vehicleService: VehicleService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.mode = 'edit';
      this.vehicleId = id;
      this.loadVehicle(id);
    }
  }

  private loadVehicle(id: string): void {
    this.loading = true;
    this.loadError = '';

    this.vehicleService.findById(id).subscribe({
      next: (vehicle: VehicleDto) => {
        this.licencePlate = vehicle.licencePlate;
        this.vin = vehicle.vin;
        this.make = vehicle.make;
        this.model = vehicle.model;
        this.year = vehicle.year;
        this.color = vehicle.color;
        this.vehicleType = vehicle.vehicleType;
        this.enginceCc = vehicle.enginceCc ?? null;
        this.fuelType = vehicle.fuelType;
        this.registrationDate = vehicle.registrationDate;
        this.registrationExpiry = vehicle.registrationExpiry;
        this.ownerId = vehicle.ownerId;
        this.isActive = vehicle.isActive;
        this.isStolen = vehicle.isStolen;
        this.loading = false;
      },
      error: () => {
        this.loadError = 'Unable to load this vehicle.';
        this.loading = false;
      }
    });
  }

  get canSubmit(): boolean {
    if (this.submitting || this.loading) return false;
    if (
      this.mode === 'create' &&
      (!this.licencePlate || !this.vin || !this.ownerId || !this.registrationDate)
    ) {
      return false;
    }
    return (
      !!this.make &&
      !!this.model &&
      !!this.year &&
      !!this.color &&
      !!this.vehicleType &&
      !!this.fuelType &&
      !!this.registrationDate &&
      !!this.registrationExpiry
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
    const request: VehicleCreateRequest = {
      licencePlate: this.licencePlate,
      vin: this.vin,
      make: this.make,
      model: this.model,
      year: this.year as number,
      color: this.color,
      vehicleType: this.vehicleType,
      enginceCc: this.enginceCc ?? undefined,
      fuelType: this.fuelType,
      registrationDate: this.registrationDate,
      registrationExpiry: this.registrationExpiry,
      ownerId: this.ownerId
    };

    this.submitting = true;
    this.submitError = '';

    this.vehicleService.create(request).subscribe({
      next: () => {
        this.submitting = false;
        this.router.navigate(['/admin/vehicles']);
      },
      error: (err) => {
        this.submitting = false;
        this.submitError = err?.error?.message ?? 'Unable to create this vehicle.';
      }
    });
  }

  private submitUpdate(): void {
    const request: VehicleUpdateRequest = {
      make: this.make,
      model: this.model,
      year: this.year as number,
      color: this.color,
      vehicleType: this.vehicleType,
      fuelType: this.fuelType,
      registrationDate: this.registrationDate,
      registrationExpiry: this.registrationExpiry,
      isActive: this.isActive
    };

    this.submitting = true;
    this.submitError = '';

    this.vehicleService.update(this.vehicleId, request).subscribe({
      next: () => {
        this.submitting = false;
        this.router.navigate(['/admin/vehicles']);
      },
      error: (err) => {
        this.submitting = false;
        this.submitError = err?.error?.message ?? 'Unable to update this vehicle.';
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/vehicles']);
  }
}
