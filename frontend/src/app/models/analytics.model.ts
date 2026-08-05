import { AutoIdEntity, UuidEntity } from './common.model';
import { PeriodType, ViolationType } from './enums';

/** StatisticDto (2.20) — mirrors SystemStatisticsEntity (2.21). */
export interface StatisticDto {
  periodType: PeriodType;
  periodStart: string;
  periodEnd: string;
  totalViolations: number;
  autoDetected: number;
  manuallyRecorded: number; // doc shows "manuallyRecored" — treated as a typo, verify against the entity
  totalFinesIssued: number; // doc shows "totalFinesIssue" — treated as a typo, verify against the entity
  totalFinesAmount: number;
  totalCollected: number;
  totalOverdue: number;
  appealsSubmitted: number; // doc shows "appealSubmited" — treated as a typo, verify against the entity
  appealsApproved: number;
  activeCameras: number;
  computedAt: string;
}

/**
 * AccidentHotspotEntity (2.1) — the persisted hotspot record. HeatmapDataDto
 * (2.8) is described only as "read only object showing heatmap data
 * entity," so it's assumed to mirror this shape; narrow it down once the
 * real GET /api/analytics/heatmap response is seen.
 */
export interface AccidentHotspotEntity extends AutoIdEntity {
  latitude: number;
  longitude: number;
  radiusMeters: number;
  violationCount: number;
  dominantType: ViolationType;
  periodStart: string;
  periodEnd: string;
  severityScore: number;
  locationLabel?: string;
}

/** HeatmapDataDto (2.8) — see note above on AccidentHotspotEntity. */
export type HeatmapDataDto = Omit<AccidentHotspotEntity, keyof AutoIdEntity>;

/** ViolationLocationLogDto — mirrors ViolationLocationLogEntity (2.23). */
export interface ViolationLocationLogDto extends UuidEntity {
  latitude: number;
  longitude: number;
  violationType: ViolationType;
  occuredAt: string; // doc's literal field name (likely "occurredAt") — verify against the entity
  violationId: string;
}

/** Query params for GET /api/analytics/statistics, /heatmap, /dangerzones. */
export interface AnalyticsQuery {
  periodType?: PeriodType;
  from?: string;
  to?: string;
}