import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { CameraService } from '../services/camera.service';
import { CameraDto, CameraSearchObject } from '../models/camera.model';
import { CameraType } from '../models/enums';
import { DataTableColumn } from '../shared/data-table/data-table.component';

const CAMERA_TYPE_OPTIONS: { value: CameraType; label: string }[] = [
  { value: 'ANPR', label: 'ANPR' },
  { value: 'SPEED_RADAR', label: 'Speed radar' },
  { value: 'RED_LIGHT', label: 'Red light' },
  { value: 'MOBILE_RADAR', label: 'Mobile radar' },
  { value: 'OVERHEAD', label: 'Overhead' }
];

const PAGE_SIZE = 20;

/**
 * Admin camera fleet — GET /api/cameras (search), DELETE /api/cameras/{id}
 * (decommission/soft-delete, gated by app-confirm-dialog). Camera ids are
 * Integer per CameraDto (confirmed real — see the note in camera.model.ts),
 * so CameraService/BaseCrudService<..., number> is used throughout.
 */
@Component({
  selector: 'app-camera-list',
  standalone: false,
  templateUrl: './camera-list.component.html'
})
export class CameraListComponent implements OnInit {
  rows: CameraDto[] = [];
  loading = false;
  error = '';

  page = 0;
  hasMore = false;
  count = 0;

  search = '';
  typeFilter: CameraType | '' = '';
  onlineFilter: '' | 'true' | 'false' = '';
  activeFilter: '' | 'true' | 'false' = '';

  typeOptions = CAMERA_TYPE_OPTIONS;

  decommissionTarget: CameraDto | null = null;
  decommissioning = false;
  decommissionError = '';

  columns: DataTableColumn<CameraDto>[] = [
    { key: 'serialNumber', label: 'Serial #', mono: true },
    { key: 'name', label: 'Name' },
    { key: 'cameraType', label: 'Type', align: 'center' },
    { key: 'locationDescription', label: 'Location' },
    { key: 'isOnline', label: 'Online', align: 'center', format: (row) => (row.isOnline ? 'Online' : 'Offline') },
    { key: 'isActive', label: 'Status', align: 'center', format: (row) => (row.isActive ? 'Active' : 'Inactive') },
    {
      key: 'lastHeartbeatAt',
      label: 'Last heartbeat',
      format: (row) => (row.lastHeartbeatAt ? new Date(row.lastHeartbeatAt).toLocaleString() : '—')
    }
  ];

  constructor(private cameraService: CameraService, private router: Router) {}

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading = true;
    this.error = '';

    const searchObject: CameraSearchObject = {
      page: this.page,
      limit: PAGE_SIZE,
      includeCount: true,
      search: this.search.trim() || undefined,
      cameraType: this.typeFilter || undefined,
      isOnline: this.onlineFilter === '' ? undefined : this.onlineFilter === 'true',
      isActive: this.activeFilter === '' ? undefined : this.activeFilter === 'true'
    };

    this.cameraService.search(searchObject).subscribe({
      next: (result) => {
        this.rows = result?.resultList ?? [];
        this.count = result?.count ?? 0;
        this.hasMore = result?.hasMore ?? false;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load cameras.';
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
    this.router.navigate(['/admin/cameras/new']);
  }

  edit(camera: CameraDto): void {
    this.router.navigate(['/admin/cameras', camera.id, 'edit']);
  }

  maintenance(camera: CameraDto): void {
    this.router.navigate(['/admin/cameras', camera.id, 'maintenance']);
  }

  confirmDecommission(camera: CameraDto): void {
    this.decommissionError = '';
    this.decommissionTarget = camera;
  }

  cancelDecommission(): void {
    this.decommissionTarget = null;
  }

  get decommissionMessage(): string {
    return this.decommissionTarget
      ? `Decommission camera ${this.decommissionTarget.serialNumber}? It will be soft-deleted and stop receiving events.`
      : '';
  }

  doDecommission(): void {
    if (!this.decommissionTarget) return;

    this.decommissioning = true;
    this.decommissionError = '';

    this.cameraService.decommission(this.decommissionTarget.id).subscribe({
      next: () => {
        this.decommissioning = false;
        this.decommissionTarget = null;
        this.load();
      },
      error: () => {
        this.decommissioning = false;
        this.decommissionError = 'Unable to decommission this camera.';
      }
    });
  }
}
