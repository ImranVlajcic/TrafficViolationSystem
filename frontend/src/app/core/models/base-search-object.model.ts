/**
 * Base query params shared by every paginated search endpoint.
 * Mirrors backend `BaseSearchObject` (core/model/BaseSearchObject.java).
 *
 * Extend this per-domain and add domain-specific filters
 * (see e.g. VehicleSearchObject, ViolationSearchObject).
 *
 * TId is `number` for int-id domains (FineRule, Camera, RoadZone, SystemConfig)
 * and `string` (UUID) for uuid-id domains (User, Vehicle, Violation, Driver, ...).
 */
export interface BaseSearchObject<TId = string> {
  page?: number;
  limit?: number;
  getAll?: boolean;
  order?: string;
  orderDirection?: 'ASC' | 'DESC';
  id?: TId;
  includeDeleted?: boolean;
  includeCount?: boolean;
}
