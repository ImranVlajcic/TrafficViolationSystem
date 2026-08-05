/**
 * Shared shapes used across every domain model. Mirrors the `core` module
 * described in the backend docs (section 1): ApiResponse, BaseSearchObject,
 * PagedResult, plus the audit fields every entity carries via BaseEntity.
 */

/** Wrapper for every single-object response (core/model/ApiResponse, 1.7.1). */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

/** Wrapper for every paginated list response (core/model/PagedResult, 1.7.3). */
export interface PagedResult<T> {
  resultList: T[];
  count: number;
  hasMore: boolean;
}

/**
 * Query params for any paginated/filterable search endpoint
 * (core/model/BaseSearchObject, 1.7.2). Domain-specific search objects
 * extend this with their own filter fields.
 */
export interface BaseSearchObject<TKey = string | number> {
  page?: number;
  limit?: number;
  getAll?: boolean;
  order?: string;
  orderDirection?: 'ASC' | 'DESC';
  id?: TKey;
  includeDeleted?: boolean;
  includeCount?: boolean;
}

/** Shape returned by the global exception handler (core/config, 1.2.4). */
export interface ErrorResponse {
  data: unknown;
  code: string;
  message: string;
}

/**
 * Fields every entity extending BaseEntity carries (core/entity, 1.4.2),
 * on top of AbstractEntity's soft-delete field. Spread this into a model
 * instead of retyping created/updated bookkeeping on every interface.
 */
export interface AuditFields {
  createdDate?: string;
  updatedDate?: string;
  createdBy?: string;
  updatedBy?: string;
  deletedAt?: string | null;
}

/** Entities extending AutoIdBaseEntity (1.4.3) — internal numeric-id records. */
export interface AutoIdEntity extends AuditFields {
  id: number;
}

/** Entities extending UUIDBaseEntity (1.4.4) — externally-facing records. */
export interface UuidEntity extends AuditFields {
  id: string;
}