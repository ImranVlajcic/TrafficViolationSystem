import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { DriverService } from '../services/driver.service';
import { DriverDto, DriverCreateRequest, DriverUpdateRequest } from '../models/driver.model';

/**
 * Admin create/edit driver, reached via /admin/drivers/new and
 * /admin/drivers/:id/edit.
 *
 * licenceNumber, nationalId, dateOfBirth, and email have no field on
 * DriverUpdateRequest, so they're shown read-only in edit mode rather than
 * sent back to the server (same pattern as `username` in user-form).
 * isSuspended also isn't on DriverUpdateRequest — it's shown as a plain
 * status line in edit mode, not an editable control; suspend/lift-suspension
 * is a separate DriverService action, intentionally left out of this form.
 */
@Component({
  selector: 'app-driver-form',
  standalone: false,
  templateUrl: './driver-form.component.html',
  styleUrls: ['./driver-form.component.css']
})
export class DriverFormComponent implements OnInit {
  mode: 'create' | 'edit' = 'create';
  driverId = '';

  loading = false;
  loadError = '';
  submitting = false;
  submitError = '';

  licenceNumber = '';
  nationalId = '';
  firstName = '';
  lastName = '';
  dateOfBirth = '';
  email = '';
  phoneNumber = '';
  adress = '';
  licenseCategory = '';
  licenseIssuedAt = '';
  licenceExpiresAt = '';

  // Display-only in edit mode — not part of DriverUpdateRequest.
  isSuspended = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private driverService: DriverService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.mode = 'edit';
      this.driverId = id;
      this.loadDriver(id);
    }
  }

  private loadDriver(id: string): void {
    this.loading = true;
    this.loadError = '';

    this.driverService.findById(id).subscribe({
      next: (driver: DriverDto) => {
        this.licenceNumber = driver.licenceNumber;
        this.nationalId = driver.nationalId;
        this.firstName = driver.firstName;
        this.lastName = driver.lastName;
        this.dateOfBirth = driver.dateOfBirth;
        this.email = driver.email;
        this.phoneNumber = driver.phoneNumber;
        this.adress = driver.adress;
        this.licenseCategory = driver.licenseCategory;
        this.licenseIssuedAt = driver.licenseIssuedAt;
        this.licenceExpiresAt = driver.licenceExpiresAt;
        this.isSuspended = driver.isSuspended;
        this.loading = false;
      },
      error: () => {
        this.loadError = 'Unable to load this driver.';
        this.loading = false;
      }
    });
  }

  get canSubmit(): boolean {
    if (this.submitting || this.loading) return false;
    if (
      this.mode === 'create' &&
      (!this.licenceNumber || !this.nationalId || !this.dateOfBirth || !this.email)
    ) {
      return false;
    }
    return (
      !!this.firstName &&
      !!this.lastName &&
      !!this.phoneNumber &&
      !!this.adress &&
      !!this.licenseCategory &&
      !!this.licenseIssuedAt &&
      !!this.licenceExpiresAt
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
    const request: DriverCreateRequest = {
      licenceNumber: this.licenceNumber,
      nationalId: this.nationalId,
      firstName: this.firstName,
      lastName: this.lastName,
      dateOfBirth: this.dateOfBirth,
      email: this.email,
      phoneNumber: this.phoneNumber,
      adress: this.adress,
      licenseCategory: this.licenseCategory,
      licenseIssuedAt: this.licenseIssuedAt,
      licenceExpiresAt: this.licenceExpiresAt
    };

    this.submitting = true;
    this.submitError = '';

    this.driverService.create(request).subscribe({
      next: () => {
        this.submitting = false;
        this.router.navigate(['/admin/drivers']);
      },
      error: (err) => {
        this.submitting = false;
        this.submitError = err?.error?.message ?? 'Unable to create this driver.';
      }
    });
  }

  private submitUpdate(): void {
    const request: DriverUpdateRequest = {
      firstName: this.firstName,
      lastName: this.lastName,
      phoneNumber: this.phoneNumber,
      adress: this.adress,
      licenseCategory: this.licenseCategory,
      licenseIssuedAt: this.licenseIssuedAt,
      licenceExpiresAt: this.licenceExpiresAt
    };

    this.submitting = true;
    this.submitError = '';

    this.driverService.update(this.driverId, request).subscribe({
      next: () => {
        this.submitting = false;
        this.router.navigate(['/admin/drivers']);
      },
      error: (err) => {
        this.submitting = false;
        this.submitError = err?.error?.message ?? 'Unable to update this driver.';
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/drivers']);
  }
}
