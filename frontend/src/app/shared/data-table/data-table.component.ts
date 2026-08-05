import { Component, ContentChild, EventEmitter, Input, Output, TemplateRef } from '@angular/core';

export type SortDirection = 'ASC' | 'DESC';

export interface DataTableColumn<T = any> {
  /** Row property to read (dot-path supported, e.g. 'driver.lastName'). */
  key: string;
  label: string;
  align?: 'left' | 'right' | 'center';
  sortable?: boolean;
  /** Optional custom cell formatter; falls back to the raw field value. */
  format?: (row: T) => string;
  /** Render the value with the mono font (good for ids, amounts, codes). */
  mono?: boolean;
}

export interface SortChange {
  column: string;
  direction: SortDirection;
}

/**
 * Generic paginated table driven by column definitions, matching the
 * backend's PagedResult<T> shape (resultList / count / hasMore — there's
 * no total-page-count from the API, so pagination is Previous/Next only).
 *
 * Row actions (edit/delete/etc buttons) are projected in via a template
 * reference passed as content:
 *
 *   <app-data-table [columns]="columns" [rows]="rows" ...>
 *     <ng-template #rowActions let-row>
 *       <button class="btn btn-sm btn-outline-secondary" (click)="edit(row)">Edit</button>
 *     </ng-template>
 *   </app-data-table>
 */
@Component({
  selector: 'app-data-table',
  standalone: false,
  templateUrl: './data-table.component.html',
  styleUrls: ['./data-table.component.css']
})
export class DataTableComponent<T = any> {
  @Input() columns: DataTableColumn<T>[] = [];
  @Input() rows: T[] = [];
  @Input() loading = false;
  @Input() error: string | null = null;
  @Input() emptyMessage = 'No records found.';

  @Input() page = 1;
  @Input() hasMore = false;
  @Input() count = 0;

  @Input() sortColumn: string | null = null;
  @Input() sortDirection: SortDirection = 'ASC';

  @Output() pageChange = new EventEmitter<number>();
  @Output() sortChange = new EventEmitter<SortChange>();
  @Output() rowClick = new EventEmitter<T>();

  @ContentChild('rowActions') rowActionsTemplate?: TemplateRef<{ $implicit: T }>;

  cellValue(row: T, column: DataTableColumn<T>): string {
    if (column.format) {
      return column.format(row);
    }
    const value = column.key.split('.').reduce<any>((acc, key) => acc?.[key], row);
    return value === null || value === undefined ? '—' : String(value);
  }

  onHeaderClick(column: DataTableColumn<T>): void {
    if (!column.sortable) {
      return;
    }
    const direction: SortDirection =
      this.sortColumn === column.key && this.sortDirection === 'ASC' ? 'DESC' : 'ASC';
    this.sortChange.emit({ column: column.key, direction });
  }

  onPrevious(): void {
    if (this.page > 1) {
      this.pageChange.emit(this.page - 1);
    }
  }

  onNext(): void {
    if (this.hasMore) {
      this.pageChange.emit(this.page + 1);
    }
  }
}
