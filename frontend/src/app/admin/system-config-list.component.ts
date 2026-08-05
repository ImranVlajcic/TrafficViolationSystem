import { Component, OnInit } from '@angular/core';

// Adjust these import paths to match your actual project layout.
import { SystemConfigService } from '../services/system-config.service';
import {
  SystemConfigDto,
  SystemConfigSearchObject,
  SystemConfigUpdateRequest
} from '../models/systemconfig.model';
import { ConfigDataType } from '../models/enums';

const DATA_TYPE_OPTIONS: { value: ConfigDataType; label: string }[] = [
  { value: 'STRING', label: 'String' },
  { value: 'INTEGER', label: 'Integer' },
  { value: 'DECIMAL', label: 'Decimal' },
  { value: 'BOOLEAN', label: 'Boolean' },
  { value: 'JSON', label: 'JSON' }
];

const PAGE_SIZE = 20;

/**
 * Admin system config — GET /api/config (search), PUT /api/config/{id}.
 * SystemConfigUpdateRequest is configValue + optional description — that
 * is the *entire* writable surface. configKey/category/dataType/
 * isEditable are read-only metadata (seeded server-side), so there's no
 * create/delete UI here, only in-place editing of entries that already
 * exist and have isEditable: true.
 *
 * configValue is always a string on the wire regardless of dataType —
 * dataType is metadata telling the UI how to *render* it (number input,
 * true/false select, JSON textarea, etc.), not a separate typed column.
 *
 * Deliberately bypasses app-data-table: inline-editing a cell needs a
 * real form control that varies per row (by dataType), and the shared
 * table only supports a per-row *actions* template, not per-cell
 * content — so this is a plain Bootstrap table instead.
 */
@Component({
  selector: 'app-system-config-list',
  standalone: false,
  templateUrl: './system-config-list.component.html',
  styleUrls: ['./system-config-list.component.css']
})
export class SystemConfigListComponent implements OnInit {
  rows: SystemConfigDto[] = [];
  loading = false;
  error = '';

  page = 1;
  hasMore = false;
  count = 0;

  categoryFilter = '';
  dataTypeFilter: ConfigDataType | '' = '';
  dataTypeOptions = DATA_TYPE_OPTIONS;

  editingId: number | null = null;
  editValue: string | number = '';
  saving = false;
  saveError = '';

  constructor(private systemConfigService: SystemConfigService) {}

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading = true;
    this.error = '';

    const searchObject: SystemConfigSearchObject = {
      page: this.page,
      limit: PAGE_SIZE,
      includeCount: true,
      category: this.categoryFilter.trim() || undefined,
      dataType: this.dataTypeFilter || undefined
    };

    this.systemConfigService.search(searchObject).subscribe({
      next: (result) => {
        this.rows = result?.resultList ?? [];
        this.count = result?.count ?? 0;
        this.hasMore = result?.hasMore ?? false;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load system config.';
        this.loading = false;
      }
    });
  }

  onFilterSubmit(): void {
    this.page = 1;
    this.load();
  }

  onPrevious(): void {
    if (this.page > 1) {
      this.page -= 1;
      this.load();
    }
  }

  onNext(): void {
    if (this.hasMore) {
      this.page += 1;
      this.load();
    }
  }

  isEditing(row: SystemConfigDto): boolean {
    return this.editingId === row.id;
  }

  startEdit(row: SystemConfigDto): void {
    if (!row.isEditable) return;
    this.saveError = '';
    this.editingId = row.id;
    this.editValue = row.configValue;
  }

  cancelEdit(): void {
    this.editingId = null;
  }

  saveEdit(row: SystemConfigDto): void {
    const request: SystemConfigUpdateRequest = {
      configValue: String(this.editValue),
      description: row.description
    };

    this.saving = true;
    this.saveError = '';

    this.systemConfigService.update(row.id, request).subscribe({
      next: (updated) => {
        row.configValue = updated.configValue;
        this.saving = false;
        this.editingId = null;
      },
      error: () => {
        this.saving = false;
        this.saveError = `Unable to update ${row.configKey}.`;
      }
    });
  }
}
