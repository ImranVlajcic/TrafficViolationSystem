import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { CameraService } from '../services/camera.service';
import { CameraDto, CameraMaintenanceLogDto, LogMaintenanceRequest } from '../models/camera.model';
import { MaintenanceType } from '../models/enums';
import { DataTableColumn } from '../shared/data-table/data-table.component';

const MAINTENANCE_TYPE_OPTIONS: { value: MaintenanceType; label: string }[] = [
  { value: 'PHYSICAL_INSPECTION', label: 'Physical inspection' },
  { value: 'CALIBRATION', label: 'Calibration' },
  { value: 'FIRMWARE_UPDATE', label: 'Firmware update' },
  { value: 'FAULT_REPAIR', label: 'Fault repair' },
  { value: 'HARDWARE_REPLACEMENT', label: 'Hardware replacement' }
];

/**
 * Admin camera maintenance, reached via /admin/cameras/:id/maintenance.
 * Two independent panels on one page:
 *
 *   - this camera's maintenance history (GET .../maintenance) plus a
 *     "log maintenance" form (POST .../maintenance) and a "mark complete"
 *     action per scheduled/incomplete entry (POST .../maintenance/{logId}/complete,
 *     gated by app-confirm-dialog, tone="default" since completing a log
 *     entry isn't destructive the way delete/decommission are);
 *   - a site-wide offline-cameras panel (GET /api/cameras/offline). This
 *     is NOT scoped to :id — it's shown regardless of which camera you're
 *     viewing, so an admin doing maintenance work always has visibility
 *     into what else in the fleet needs attention. Clicking a row jumps
 *     to that camera's own maintenance page.
 *
 * getMaintenanceHistory/getOffline/logMaintenance/completeMaintenance are
 * all "action"-style endpoints per CameraService, so each response is
 * wrapped in ApiResponse<T> — unlike the plain search()/findById() calls
 * used elsewhere, which return the DTO/PagedResult directly.
 */
@Component({
  selector: 'app-camera-maintenance',
  standalone: false,
  templateUrl: './camera-maintenance.component.html',
  styleUrls: ['./camera-maintenance.component.css']
})
export class CameraMaintenanceComponent implements OnInit {
  cameraId!: number;
  camera: CameraDto | null = null;
  loadingCamera = false;
  cameraError = '';

  history: CameraMaintenanceLogDto[] = [];
  loadingHistory = false;
  historyError = '';

  offline: CameraDto[] = [];
  loadingOffline = false;
  offlineError = '';

  typeOptions = MAINTENANCE_TYPE_OPTIONS;

  maintenanceType: MaintenanceType = 'PHYSICAL_INSPECTION';
  scheduledDate = '';
  firmwareBefore = '';
  firmwareAfter = '';
  notes = '';
  isCompleted = false;

  logging = false;
  logError = '';

  completeTarget: CameraMaintenanceLogDto | null = null;
  completing = false;
  completeError = '';

  historyColumns: DataTableColumn<CameraMaintenanceLogDto>[] = [
    { key: 'maintenanceType', label: 'Type' },
    { key: 'scheduledDate', label: 'Scheduled', format: (row) => new Date(row.scheduledDate).toLocaleDateString() },
    {
      key: 'completedAt',
      label: 'Completed',
      format: (row) => (row.completedAt ? new Date(row.completedAt).toLocaleDateString() : '—')
    },
    { key: 'firmwareBefore', label: 'FW before', mono: true },
    { key: 'firmwareAfter', label: 'FW after', mono: true },
    {
      key: 'isCompleted',
      label: 'Status',
      align: 'center',
      format: (row) => (row.isCompleted ? 'Completed' : 'Scheduled')
    }
  ];

  offlineColumns: DataTableColumn<CameraDto>[] = [
    { key: 'serialNumber', label: 'Serial #', mono: true },
    { key: 'name', label: 'Name' },
    { key: 'cameraType', label: 'Type', align: 'center' },
    {
      key: 'lastHeartbeatAt',
      label: 'Last heartbeat',
      format: (row) => (row.lastHeartbeatAt ? new Date(row.lastHeartbeatAt).toLocaleString() : 'Never')
    }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private cameraService: CameraService
  ) {}

  ngOnInit(): void {
    this.cameraId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadCamera();
    this.loadHistory();
    this.loadOffline();
  }

  private loadCamera(): void {
    this.loadingCamera = true;
    this.cameraError = '';

    this.cameraService.findById(this.cameraId).subscribe({
      next: (camera) => {
        this.camera = camera;
        this.loadingCamera = false;
      },
      error: () => {
        this.cameraError = 'Unable to load this camera.';
        this.loadingCamera = false;
      }
    });
  }

  private loadHistory(): void {
    this.loadingHistory = true;
    this.historyError = '';

    this.cameraService.getMaintenanceHistory(this.cameraId).subscribe({
      next: (response) => {
        this.history = response?.data ?? [];
        this.loadingHistory = false;
      },
      error: () => {
        this.historyError = 'Unable to load maintenance history.';
        this.loadingHistory = false;
      }
    });
  }

  private loadOffline(): void {
    this.loadingOffline = true;
    this.offlineError = '';

    this.cameraService.getOffline().subscribe({
      next: (response) => {
        this.offline = response?.data ?? [];
        this.loadingOffline = false;
      },
      error: () => {
        this.offlineError = 'Unable to load offline cameras.';
        this.loadingOffline = false;
      }
    });
  }

  get canLog(): boolean {
    return !this.logging && !!this.maintenanceType && !!this.scheduledDate;
  }

  logMaintenance(): void {
    if (!this.canLog) return;

    const request: LogMaintenanceRequest = {
      maintenanceType: this.maintenanceType,
      scheduledDate: this.scheduledDate,
      firmwareBefore: this.firmwareBefore || undefined,
      firmwareAfter: this.firmwareAfter || undefined,
      notes: this.notes || undefined,
      isCompleted: this.isCompleted
    };

    this.logging = true;
    this.logError = '';

    this.cameraService.logMaintenance(this.cameraId, request).subscribe({
      next: () => {
        this.logging = false;
        this.resetForm();
        this.loadHistory();
      },
      error: (err) => {
        this.logging = false;
        this.logError = err?.error?.message ?? 'Unable to log this maintenance entry.';
      }
    });
  }

  private resetForm(): void {
    this.maintenanceType = 'PHYSICAL_INSPECTION';
    this.scheduledDate = '';
    this.firmwareBefore = '';
    this.firmwareAfter = '';
    this.notes = '';
    this.isCompleted = false;
  }

  confirmComplete(log: CameraMaintenanceLogDto): void {
    this.completeError = '';
    this.completeTarget = log;
  }

  cancelComplete(): void {
    this.completeTarget = null;
  }

  get completeMessage(): string {
    if (!this.completeTarget) return '';
    const label = this.completeTarget.maintenanceType.toLowerCase().replace(/_/g, ' ');
    return `Mark this ${label} entry as completed?`;
  }

  doComplete(): void {
    if (!this.completeTarget) return;

    this.completing = true;
    this.completeError = '';

    this.cameraService.completeMaintenance(this.cameraId, this.completeTarget.id).subscribe({
      next: () => {
        this.completing = false;
        this.completeTarget = null;
        this.loadHistory();
      },
      error: () => {
        this.completing = false;
        this.completeError = 'Unable to mark this entry complete.';
      }
    });
  }

  goToCamera(camera: CameraDto): void {
    this.router.navigate(['/admin/cameras', camera.id, 'maintenance']);
  }

  backToCameras(): void {
    this.router.navigate(['/admin/cameras']);
  }
}
