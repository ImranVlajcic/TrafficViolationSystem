import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

// Adjust these import paths to match your actual project layout.
import { RoadZoneService } from '../services/road-zone.service';
import { RoadZoneDto, RoadZoneSearchObject } from '../models/roadzone.model';
import { ZoneType } from '../models/enums';
import { DataTableColumn } from '../shared/data-table/data-table.component';

// RESIEDNTAL is confirmed real (see enums.ts), kept literal as the value —
// only the display label is spelled correctly.
const ZONE_TYPE_OPTIONS: { value: ZoneType; label: string }[] = [
  { value: 'SCHOOL', label: 'School' },
  { value: 'RESIEDNTAL', label: 'Residential' },
  { value: 'HIGHWAY', label: 'Highway' },
  { value: 'CITY_CENTER', label: 'City center' },
  { value: 'HOSPITAL', label: 'Hospital' },
  { value: 'CONSTRUCTION', label: 'Construction' },
  { value: 'INDUSTRIAL', label: 'Industrial' }
];

const PAGE_SIZE = 20;

/**
 * Admin road zones — GET /api/zones (search), DELETE /api/zones/{id}
 * (soft-delete, clears camera assignments — gated by app-confirm-dialog).
 *
 * NOTE: RoadZoneDto extends UuidEntity (string id) in roadzone.model.ts,
 * but RoadZoneService's BaseCrudService<...> and every custom method
 * (deleteZone, assignCamera, unassignCamera) is typed to `number`. Going
 * with `number` here since it's what the service actually compiles
 * against — verify against RoadZoneEntity.java and fix whichever file is
 * wrong.
 */
@Component({
  selector: 'app-road-zone-list',
  standalone: false,
  templateUrl: './road-zone-list.component.html'
})
export class RoadZoneListComponent implements OnInit {
  rows: RoadZoneDto[] = [];
  loading = false;
  error = '';

  page = 0;
  hasMore = false;
  count = 0;

  search = '';
  typeFilter: ZoneType | '' = '';
  activeFilter: '' | 'true' | 'false' = '';

  typeOptions = ZONE_TYPE_OPTIONS;

  deleteTarget: RoadZoneDto | null = null;
  deleting = false;
  deleteError = '';

  columns: DataTableColumn<RoadZoneDto>[] = [
    { key: 'name', label: 'Name' },
    { key: 'zoneType', label: 'Type', align: 'center' },
    { key: 'speedLimitKmh', label: 'Speed limit', align: 'right' },
    {
      key: 'centraLatitude',
      label: 'Center',
      mono: true,
      format: (row) => `${row.centraLatitude.toFixed(5)}, ${row.centerLongitude.toFixed(5)}`
    },
    { key: 'radisuMeters', label: 'Radius (m)', align: 'right' },
    { key: 'isActive', label: 'Status', align: 'center', format: (row) => (row.isActive ? 'Active' : 'Inactive') }
  ];

  constructor(private roadZoneService: RoadZoneService, private router: Router) {}

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading = true;
    this.error = '';

    const searchObject: RoadZoneSearchObject = {
      page: this.page,
      limit: PAGE_SIZE,
      includeCount: true,
      search: this.search.trim() || undefined,
      zoneType: this.typeFilter || undefined,
      isActive: this.activeFilter === '' ? undefined : this.activeFilter === 'true'
    };

    this.roadZoneService.search(searchObject).subscribe({
      next: (result) => {
        this.rows = result?.resultList ?? [];
        this.count = result?.count ?? 0;
        this.hasMore = result?.hasMore ?? false;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load road zones.';
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
    this.router.navigate(['/admin/road-zones/new']);
  }

  edit(zone: RoadZoneDto): void {
    this.router.navigate(['/admin/road-zones', zone.id, 'edit']);
  }

  confirmDelete(zone: RoadZoneDto): void {
    this.deleteError = '';
    this.deleteTarget = zone;
  }

  cancelDelete(): void {
    this.deleteTarget = null;
  }

  get deleteMessage(): string {
    return this.deleteTarget
      ? `Delete zone "${this.deleteTarget.name}"? Any cameras assigned to it will be unassigned.`
      : '';
  }

  doDelete(): void {
    if (!this.deleteTarget) return;

    this.deleting = true;
    this.deleteError = '';

    this.roadZoneService.deleteZone(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleting = false;
        this.deleteTarget = null;
        this.load();
      },
      error: () => {
        this.deleting = false;
        this.deleteError = 'Unable to delete this zone.';
      }
    });
  }
}
