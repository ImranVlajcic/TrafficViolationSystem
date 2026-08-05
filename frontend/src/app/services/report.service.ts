import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApiResponse } from '../core/models/api-response.model';
import { PagedResult } from '../core/models/paged-result.model';
import { buildHttpParams } from '../core/services/http-params-builder.util';

// Adjust these import paths to match your actual model locations.
import { ReportDto } from '../models/report.model';
import { ReportRequestDto } from '../models/report.model';
import { ReportSearchObject } from '../models/report.model';

// TODO: point this at your actual API base — swap for environment.apiUrl
const API_BASE = '/api/reports';

/**
 * No update/delete — reports are requested once, generated async, then
 * downloaded. Doesn't extend BaseCrudService since there's no PUT and the
 * "create" response is wrapped in ApiResponse (ApiResponseReportDto).
 */
@Injectable({ providedIn: 'root' })
export class ReportService {
  constructor(private readonly http: HttpClient) {}

  /** GET /api/reports */
  search(searchObject: ReportSearchObject = {}): Observable<PagedResult<ReportDto>> {
    const params = buildHttpParams(searchObject as unknown as Record<string, unknown>);
    return this.http.get<PagedResult<ReportDto>>(API_BASE, { params });
  }

  /**
   * GET /api/reports/{id} — report status. Always JSON.
   * Poll this until `ready === true`, then call downloadFile(id).
   */
  findById(id: string): Observable<ReportDto> {
    return this.http.get<ReportDto>(`${API_BASE}/${id}`);
  }

  /**
   * POST /api/reports — request a new report.
   * Creates a PENDING report and fires async generation, returning the
   * PENDING ReportDto immediately — poll findById(id) until ready.
   */
  requestReport(request: ReportRequestDto): Observable<ApiResponse<ReportDto>> {
    return this.http.post<ApiResponse<ReportDto>>(API_BASE, request);
  }

  /**
   * GET /api/reports/{id}/download — streams the generated PDF/CSV file.
   * Returns 404 if status isn't DONE or the file no longer exists on disk —
   * check `ready === true` on the ReportDto first.
   */
  downloadFile(id: string): Observable<Blob> {
    return this.http.get(`${API_BASE}/${id}/download`, { responseType: 'blob' });
  }

  /** GET /api/reports/my — current user's own report history. */
  getMyReports(): Observable<ApiResponse<ReportDto[]>> {
    return this.http.get<ApiResponse<ReportDto[]>>(`${API_BASE}/my`);
  }
}
