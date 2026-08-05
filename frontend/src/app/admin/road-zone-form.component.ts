import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { RoadZoneService } from '../services/road-zone.service';
import { RoadZoneDto, RoadZoneCreateRequest, RoadZoneUpdateRequest } from '../models/roadzone.model';
import { ZoneType } from '../models/enums';

const ZONE_TYPE_OPTIONS: { value: ZoneType; label: string }[] = [
  { value: 'SCHOOL', label: 'School' },
  { value: 'RESIEDNTAL', label: 'Residential' },
  { value: 'HIGHWAY', label: 'Highway' },
  { value: 'CITY_CENTER', label: 'City center' },
  { value: 'HOSPITAL', label: 'Hospital' },
  { value: 'CONSTRUCTION', label: 'Construction' },
  { value: 'INDUSTRIAL', label: 'Industrial' }
];

/**
 * Admin create/edit road zone, reached via /admin/road-zones/new and
 * /admin/road-zones/:id/edit.
 *
 * geoJsonBoundary is a raw textarea for now — the task notes a map-draw
 * tool would be nicer here later; this just validates it's parseable
 * JSON before submit so a malformed paste doesn't reach the backend.
 *
 * Field names (centraLatitude, radisuMeters) are kept exactly as spelled
 * in roadzone.model.ts — see that file's note on why. Labels below are
 * spelled correctly; only the bound property names carry the typo.
 *
 * isActive is edit-only (absent from RoadZoneCreateRequest) — same
 * create-vs-update split used on the camera form.
 */
@Component({
  selector: 'app-road-zone-form',
  standalone: false,
  templateUrl: './road-zone-form.component.html',
  styleUrls: ['./road-zone-form.component.css']
})
export class RoadZoneFormComponent implements OnInit {
  mode: 'create' | 'edit' = 'create';
  zoneId!: number;

  loading = false;
  loadError = '';
  submitting = false;
  submitError = '';
  boundaryError = '';

  typeOptions = ZONE_TYPE_OPTIONS;

  name = '';
  zoneType: ZoneType = 'SCHOOL';
  speedLimitKmh: number | null = null;
  description = '';
  centraLatitude: number | null = null;
  centerLongitude: number | null = null;
  radisuMeters: number | null = null;
  geoJsonBoundary = '';
  isActive = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private roadZoneService: RoadZoneService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.mode = 'edit';
      this.zoneId = Number(idParam);
      this.loadZone(this.zoneId);
    }
  }

  private loadZone(id: number): void {
    this.loading = true;
    this.loadError = '';

    this.roadZoneService.findById(id).subscribe({
      next: (zone: RoadZoneDto) => {
        this.name = zone.name;
        this.zoneType = zone.zoneType;
        this.speedLimitKmh = zone.speedLimitKmh;
        this.description = zone.description ?? '';
        this.centraLatitude = zone.centraLatitude;
        this.centerLongitude = zone.centerLongitude;
        this.radisuMeters = zone.radisuMeters;
        this.geoJsonBoundary = zone.geoJsonBoundary ?? '';
        this.isActive = zone.isActive;
        this.loading = false;
      },
      error: () => {
        this.loadError = 'Unable to load this zone.';
        this.loading = false;
      }
    });
  }

  get canSubmit(): boolean {
    if (this.submitting || this.loading) return false;
    return (
      !!this.name &&
      this.speedLimitKmh !== null &&
      this.centraLatitude !== null &&
      this.centerLongitude !== null &&
      this.radisuMeters !== null
    );
  }

  /** Returns false (and sets boundaryError) if the boundary text isn't valid JSON. */
  private validateBoundary(): boolean {
    this.boundaryError = '';
    if (!this.geoJsonBoundary.trim()) {
      return true;
    }
    try {
      JSON.parse(this.geoJsonBoundary);
      return true;
    } catch {
      this.boundaryError = 'GeoJSON boundary must be valid JSON.';
      return false;
    }
  }

  submit(): void {
    if (!this.validateBoundary()) return;

    if (this.mode === 'create') {
      this.submitCreate();
    } else {
      this.submitUpdate();
    }
  }

  private submitCreate(): void {
    const request: RoadZoneCreateRequest = {
      name: this.name,
      zoneType: this.zoneType,
      speedLimitKmh: this.speedLimitKmh as number,
      description: this.description || undefined,
      centraLatitude: this.centraLatitude as number,
      centerLongitude: this.centerLongitude as number,
      radisuMeters: this.radisuMeters as number,
      geoJsonBoundary: this.geoJsonBoundary || undefined
    };

    this.submitting = true;
    this.submitError = '';

    this.roadZoneService.create(request).subscribe({
      next: () => {
        this.submitting = false;
        this.router.navigate(['/admin/road-zones']);
      },
      error: (err) => {
        this.submitting = false;
        this.submitError = err?.error?.message ?? 'Unable to create this zone.';
      }
    });
  }

  private submitUpdate(): void {
    const request: RoadZoneUpdateRequest = {
      name: this.name,
      zoneType: this.zoneType,
      speedLimitKmh: this.speedLimitKmh as number,
      description: this.description || undefined,
      isActive: this.isActive,
      centraLatitude: this.centraLatitude as number,
      centerLongitude: this.centerLongitude as number,
      radisuMeters: this.radisuMeters as number,
      geoJsonBoundary: this.geoJsonBoundary || undefined
    };

    this.submitting = true;
    this.submitError = '';

    this.roadZoneService.update(this.zoneId, request).subscribe({
      next: () => {
        this.submitting = false;
        this.router.navigate(['/admin/road-zones']);
      },
      error: (err) => {
        this.submitting = false;
        this.submitError = err?.error?.message ?? 'Unable to update this zone.';
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/road-zones']);
  }
}
