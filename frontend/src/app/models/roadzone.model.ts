import { AutoIdEntity, BaseSearchObject, UuidEntity } from './common.model';
import { ZoneType } from './enums';

/**
 * RoadZoneDto (12.3) — mirrors RoadZoneEntity (12.4). The doc's field
 * names "centraLatitude" and "radisuMeters" look like typos, but they're
 * repeated identically across the entity, create request, and update
 * request sections — consistent repetition across 3 independent sections
 * suggests these were copy-typed from the real Java fields, not a one-off
 * slip. Kept literal; verify against RoadZoneEntity.java either way.
 */
export interface RoadZoneDto extends AutoIdEntity {
  name: string;
  zoneType: ZoneType;
  speedLimitKmh: number;
  description?: string;
  isActive: boolean;
  centraLatitude: number;
  centerLongitude: number;
  radisuMeters: number;
  geoJsonBoundary?: string;
}

/** RoadZoneCreateRequest (12.2). */
export interface RoadZoneCreateRequest {
  name: string;
  zoneType: ZoneType;
  speedLimitKmh: number;
  description?: string;
  centraLatitude: number;
  centerLongitude: number;
  radisuMeters: number;
  geoJsonBoundary?: string;
}

/** RoadZoneUpdateRequest (12.9). */
export interface RoadZoneUpdateRequest {
  name?: string;
  zoneType?: ZoneType;
  speedLimitKmh?: number;
  description?: string;
  isActive?: boolean;
  centraLatitude?: number;
  centerLongitude?: number;
  radisuMeters?: number;
  geoJsonBoundary?: string;
}

/** RoadZoneSearchObject (12.7) filter fields, combine with BaseSearchObject. */
export interface RoadZoneSearchObject extends BaseSearchObject {
  search?: string;
  zoneType?: ZoneType;
  isActive?: boolean;
}