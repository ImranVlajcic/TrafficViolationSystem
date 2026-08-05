import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApiResponse } from '../core/models/api-response.model';
import { PagedResult } from '../core/models/paged-result.model';
import { buildHttpParams } from '../core/services/http-params-builder.util';

// Adjust these import paths to match your actual model locations.
import { AuditLogDto } from '../models/auditlog.model';
import { AuditLogSearchObject } from '../models/auditlog.model';

// TODO: point this at your actual API base — swap for environment.apiUrl
const API_BASE = '/api/audit';

/** ADMIN only. Fully read-only — the audit trail is immutable, so no create/update/delete. */
@Injectable({ providedIn: 'root' })
export class AuditLogService {
  constructor(private readonly http: HttpClient) {}

  /** GET /api/audit */
  search(searchObject: AuditLogSearchObject = {}): Observable<PagedResult<AuditLogDto>> {
    const params = buildHttpParams(searchObject as unknown as Record<string, unknown>);
    return this.http.get<PagedResult<AuditLogDto>>(API_BASE, { params });
  }

  /** GET /api/audit/{id} */
  findById(id: string): Observable<AuditLogDto> {
    return this.http.get<AuditLogDto>(`${API_BASE}/${id}`);
  }

  /**
   * GET /api/audit/entity/{type}/{entityId} — full audit history for a
   * specific record. `type` is the simple class name, e.g. 'FineEntity',
   * 'ViolationEntity'.
   */
  getForEntity(type: string, entityId: string): Observable<ApiResponse<AuditLogDto[]>> {
    return this.http.get<ApiResponse<AuditLogDto[]>>(`${API_BASE}/entity/${type}/${entityId}`);
  }

  /**
   * GET /api/audit/actor/{userId} — complete action history for a
   * specific officer or admin. Useful for accountability reviews.
   */
  getForActor(userId: string): Observable<ApiResponse<AuditLogDto[]>> {
    return this.http.get<ApiResponse<AuditLogDto[]>>(`${API_BASE}/actor/${userId}`);
  }
}
