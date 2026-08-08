import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { VehicleService } from '../services/vehicle.service';
import { VehicleDto, VehicleOwnershipHistoryDto, TransferOwnershipRequest } from '../models/vehicle.model';

/**
 * Admin vehicle ownership transfer, routed at
 * /admin/vehicles/:id/transfer-ownership. Combines two panels on one page,
 * same reasoning as camera-maintenance: the transfer form itself, plus a
 * read-only ownership history panel (GET .../ownership-history) so the
 * admin can see prior transfers while making a new one.
 */
@Component({
  selector: 'app-vehicle-transfer',
  standalone: false,
  templateUrl: './vehicle-transfer.component.html',
  styleUrls: ['./vehicle-transfer.component.css']
})
export class VehicleTransferComponent implements OnInit {
  vehicleId = '';
  vehicle: VehicleDto | null = null;
  loading = false;
  loadError = '';

  history: VehicleOwnershipHistoryDto[] = [];
  historyLoading = false;
  historyError = '';

  newOwnerId = '';
  transferDate = '';
  notes = '';

  submitting = false;
  submitError = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private vehicleService: VehicleService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/admin/vehicles']);
      return;
    }

    this.vehicleId = id;
    this.loadVehicle();
    this.loadHistory();
  }

  private loadVehicle(): void {
    this.loading = true;
    this.loadError = '';

    this.vehicleService.findById(this.vehicleId).subscribe({
      next: (vehicle) => {
        this.vehicle = vehicle;
        this.loading = false;
      },
      error: () => {
        this.loadError = 'Unable to load this vehicle.';
        this.loading = false;
      }
    });
  }

  private loadHistory(): void {
    this.historyLoading = true;
    this.historyError = '';

    this.vehicleService.getOwnershipHistory(this.vehicleId).subscribe({
      next: (response) => {
        this.history = response?.data ?? [];
        this.historyLoading = false;
      },
      error: () => {
        this.historyError = 'Unable to load ownership history.';
        this.historyLoading = false;
      }
    });
  }

  get canSubmit(): boolean {
    return !this.submitting && !!this.newOwnerId && !!this.transferDate;
  }

  submit(): void {
    const request: TransferOwnershipRequest = {
      newOwnerId: this.newOwnerId,
      transferDate: this.transferDate,
      notes: this.notes || undefined
    };

    this.submitting = true;
    this.submitError = '';

    this.vehicleService.transferOwnership(this.vehicleId, request).subscribe({
      next: () => {
        this.submitting = false;
        this.router.navigate(['/admin/vehicles']);
      },
      error: (err) => {
        this.submitting = false;
        this.submitError = err?.error?.message ?? 'Unable to transfer ownership.';
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/vehicles']);
  }
}
