import { BaseSearchObject, UuidEntity } from './common.model';
import { ReportFormat, ReportStatus, ReportType } from './enums';

/** ReportDto (2.11) — mirrors GeneratedReportEntity (2.6). */
export interface ReportDto extends UuidEntity {
  reportType: ReportType;
  format: ReportFormat;
  periodStart: string;
  periodEnd: string;
  filePath?: string;
  status: ReportStatus;
  requestedById: string;
  completedAt?: string;
  errorMessage?: string;
  parameters?: Record<string, unknown>;
}

/**
 * ReportRequestDto (2.15) — the doc literally describes this as "Read only
 * object showing appeal entity," which is very likely a copy/paste slip
 * in the doc (this section is about reports, not appeals). Modeled here
 * as the payload for POST /api/reports based on ReportGenerationService's
 * documented method signatures instead.
 */
export interface ReportRequestDto {
  reportType: ReportType;
  format: ReportFormat;
  periodStart: string;
  periodEnd: string;
}

/** ReportSearchObject (2.16) filter fields, combine with BaseSearchObject. */
export interface ReportSearchObject extends BaseSearchObject {
  status?: ReportStatus;
  reportType?: ReportType;
  format?: ReportFormat;
  requestedById?: string;
  fromDate?: string;
  toDate?: string;
}