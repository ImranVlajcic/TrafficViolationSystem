import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApiResponse } from '../core/models/api-response.model';
import { PagedResult } from '../core/models/paged-result.model';
import { buildHttpParams } from '../core/services/http-params-builder.util';

// Adjust these import paths to match your actual model locations.
import { FineDto } from '../models/fine.model';
import { FineSearchObject } from '../models/fine.model';

// TODO: point this at your actual API base — swap for environment.apiUrl
const API_BASE = '/api/fines';

/**
 * No create/update here on purpose: fines are never created or edited
 * directly by the frontend — they're issued automatically when a
 * violation is confirmed (see ViolationService.confirm()), and only
 * transition via cancel(). Doesn't extend BaseCrudService since that
 * assumes create/update exist.
 */
@Injectable({ providedIn: 'root' })
export class FineService {
  constructor(private readonly http: HttpClient) {}

  /** GET /api/fines */
  search(searchObject: FineSearchObject = {}): Observable<PagedResult<FineDto>> {
    const params = buildHttpParams(searchObject as unknown as Record<string, unknown>);
    return this.http.get<PagedResult<FineDto>>(API_BASE, { params });
  }

  /** GET /api/fines/{id} — full details including violation reference. */
  findById(id: string): Observable<FineDto> {
    return this.http.get<FineDto>(`${API_BASE}/${id}`);
  }

  /**
   * POST /api/fines/{id}/cancel?reason=... (OFFICER/ADMIN).
   * Reverses penalty points applied at issuance. Cannot cancel a paid fine.
   */
  cancel(id: string, reason: string): Observable<ApiResponse<FineDto>> {
    return this.http.post<ApiResponse<FineDto>>(`${API_BASE}/${id}/cancel`, null, {
      params: { reason },
    });
  }

  /**
   * GET /api/fines/{id}/pdf — official fine PDF document.
   * Returns 404 if not yet generated — check `pdfReady` on the FineDto first.
   */
  downloadPdf(id: string): Observable<Blob> {
    return this.http.get(`${API_BASE}/${id}/pdf`, { responseType: 'blob' });
  }

  /** GET /api/fines/my — the currently authenticated citizen's own fines. */
  getMyFines(): Observable<ApiResponse<FineDto[]>> {
    return this.http.get<ApiResponse<FineDto[]>>(`${API_BASE}/my`);
  }

  /** GET /api/fines/driver/{driverId} (OFFICER/ADMIN). */
  getForDriver(driverId: string): Observable<ApiResponse<FineDto[]>> {
    return this.http.get<ApiResponse<FineDto[]>>(`${API_BASE}/driver/${driverId}`);
  }
}
