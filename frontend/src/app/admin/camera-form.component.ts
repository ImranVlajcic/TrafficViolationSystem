import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { CameraService } from '../services/camera.service';
import { CameraDto, CameraCreateRequest, CameraUpdateRequest } from '../models/camera.model';
import { CameraType } from '../models/enums';

const CAMERA_TYPE_OPTIONS: { value: CameraType; label: string }[] = [
  { value: 'ANPR', label: 'ANPR' },
  { value: 'SPEED_RADAR', label: 'Speed radar' },
  { value: 'RED_LIGHT', label: 'Red light' },
  { value: 'MOBILE_RADAR', label: 'Mobile radar' },
  { value: 'OVERHEAD', label: 'Overhead' }
];

/**
 * Admin create/edit camera, reached via /admin/cameras/new and
 * /admin/cameras/:id/edit.
 *
 * serialNumber/latitude/longitude/mqttTopic are create-only, per
 * CameraCreateRequest — CameraUpdateRequest deliberately omits them (a
 * camera's physical identity/location/wiring isn't something you "edit"
 * after install; re-siting one means decommissioning and re-registering
 * it), so they're shown read-only/disabled in edit mode. isActive is
 * edit-only for the mirror-image reason: it doesn't exist on
 * CameraCreateRequest, since a camera can't be created inactive.
 */
@Component({
  selector: 'app-camera-form',
  standalone: false,
  templateUrl: './camera-form.component.html',
  styleUrls: ['./camera-form.component.css']
})
export class CameraFormComponent implements OnInit {
  mode: 'create' | 'edit' = 'create';
  cameraId!: number;

  loading = false;
  loadError = '';
  submitting = false;
  submitError = '';

  typeOptions = CAMERA_TYPE_OPTIONS;

  serialNumber = '';
  name = '';
  cameraType: CameraType = 'ANPR';
  latitude: number | null = null;
  longitude: number | null = null;
  directionDegrees: number | null = null;
  locationDescription = '';
  speedLimitKmh: number | null = null;
  mqttTopic = '';
  installDate = '';
  firmwareVersion = '';
  isActive = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private cameraService: CameraService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.mode = 'edit';
      this.cameraId = Number(idParam);
      this.loadCamera(this.cameraId);
    }
  }

  private loadCamera(id: number): void {
    this.loading = true;
    this.loadError = '';

    this.cameraService.findById(id).subscribe({
      next: (camera: CameraDto) => {
        this.serialNumber = camera.serialNumber;
        this.name = camera.name;
        this.cameraType = camera.cameraType;
        this.latitude = camera.latitude;
        this.longitude = camera.longitude;
        this.directionDegrees = camera.directionDegrees ?? null;
        this.locationDescription = camera.locationDescription ?? '';
        this.speedLimitKmh = camera.speedLimitKmh ?? null;
        this.mqttTopic = camera.mqttTopic;
        this.installDate = camera.installDate;
        this.firmwareVersion = camera.firmwareVersion ?? '';
        this.isActive = camera.isActive;
        this.loading = false;
      },
      error: () => {
        this.loadError = 'Unable to load this camera.';
        this.loading = false;
      }
    });
  }

  get canSubmit(): boolean {
    if (this.submitting || this.loading) return false;
    if (this.mode === 'create') {
      return (
        !!this.serialNumber &&
        !!this.name &&
        this.latitude !== null &&
        this.longitude !== null &&
        !!this.mqttTopic &&
        !!this.installDate
      );
    }
    return !!this.name && !!this.installDate;
  }

  submit(): void {
    if (this.mode === 'create') {
      this.submitCreate();
    } else {
      this.submitUpdate();
    }
  }

  private submitCreate(): void {
    const request: CameraCreateRequest = {
      serialNumber: this.serialNumber,
      name: this.name,
      cameraType: this.cameraType,
      latitude: this.latitude as number,
      longitude: this.longitude as number,
      directionDegrees: this.directionDegrees ?? undefined,
      locationDescription: this.locationDescription || undefined,
      speedLimitKmh: this.speedLimitKmh ?? undefined,
      mqttTopic: this.mqttTopic,
      installDate: this.installDate,
      firmwareVersion: this.firmwareVersion || undefined
    };

    this.submitting = true;
    this.submitError = '';

    this.cameraService.create(request).subscribe({
      next: () => {
        this.submitting = false;
        this.router.navigate(['/admin/cameras']);
      },
      error: (err) => {
        this.submitting = false;
        this.submitError = err?.error?.message ?? 'Unable to create this camera.';
      }
    });
  }

  private submitUpdate(): void {
    const request: CameraUpdateRequest = {
      name: this.name,
      cameraType: this.cameraType,
      directionDegrees: this.directionDegrees ?? undefined,
      locationDescription: this.locationDescription || undefined,
      speedLimitKmh: this.speedLimitKmh ?? undefined,
      firmwareVersion: this.firmwareVersion || undefined,
      installDate: this.installDate,
      isActive: this.isActive
    };

    this.submitting = true;
    this.submitError = '';

    this.cameraService.update(this.cameraId, request).subscribe({
      next: () => {
        this.submitting = false;
        this.router.navigate(['/admin/cameras']);
      },
      error: (err) => {
        this.submitting = false;
        this.submitError = err?.error?.message ?? 'Unable to update this camera.';
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/cameras']);
  }
}
