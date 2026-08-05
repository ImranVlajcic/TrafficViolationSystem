import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { BaseCrudService } from '../core/services/base-crud.service';
import { ApiResponse } from '../core/models/api-response.model';

// Adjust these import paths to match your actual model locations.
import { AppealDto } from '../models/appeal.model';
import { AppealCreateRequest } from '../models/appeal.model';
import { AppealUpdateRequest } from '../models/appeal.model';
import { AppealSearchObject } from '../models/appeal.model';
import { ReviewAppealRequest } from '../models/appeal.model';

// TODO: point this at your actual API base — swap for environment.apiUrl
const API_BASE = '/api/appeals';

/** No delete endpoint exists for appeals — withdraw() is the citizen-facing equivalent. */
@Injectable({ providedIn: 'root' })
export class AppealService extends BaseCrudService<
  AppealDto,
  AppealCreateRequest,
  AppealUpdateRequest,
  AppealSearchObject,
  string
> {
  constructor(http: HttpClient) {
    super(http, API_BASE);
  }

  /**
   * POST /api/appeals/{id}/withdraw (CITIZEN only).
   * Only available while the appeal is SUBMITTED. Reinstates the fine to UNPAID.
   */
  withdraw(id: string): Observable<ApiResponse<AppealDto>> {
    return this.http.post<ApiResponse<AppealDto>>(`${this.basePath}/${id}/withdraw`, {});
  }

  /**
   * POST /api/appeals/{id}/start-review (OFFICER/ADMIN).
   * Transitions SUBMITTED -> UNDER_REVIEW and assigns the reviewing officer.
   */
  startReview(id: string): Observable<ApiResponse<AppealDto>> {
    return this.http.post<ApiResponse<AppealDto>>(`${this.basePath}/${id}/start-review`, {});
  }

  /**
   * POST /api/appeals/{id}/reject (OFFICER/ADMIN).
   * Transitions SUBMITTED/UNDER_REVIEW -> REJECTED. Fine reinstated to UNPAID.
   * reviewNotes are mandatory.
   */
  reject(id: string, request: ReviewAppealRequest): Observable<ApiResponse<AppealDto>> {
    return this.http.post<ApiResponse<AppealDto>>(`${this.basePath}/${id}/reject`, request);
  }

  /**
   * POST /api/appeals/{id}/approve (OFFICER/ADMIN).
   * Transitions SUBMITTED/UNDER_REVIEW -> APPROVED. Fine cancelled, points reversed.
   * reviewNotes are mandatory.
   */
  approve(id: string, request: ReviewAppealRequest): Observable<ApiResponse<AppealDto>> {
    return this.http.post<ApiResponse<AppealDto>>(`${this.basePath}/${id}/approve`, request);
  }

  /** GET /api/appeals/pending (OFFICER/ADMIN) — SUBMITTED appeals, oldest first. */
  getPendingQueue(): Observable<ApiResponse<AppealDto[]>> {
    return this.http.get<ApiResponse<AppealDto[]>>(`${this.basePath}/pending`);
  }

  /** GET /api/appeals/driver/{driverId} */
  getForDriver(driverId: string): Observable<ApiResponse<AppealDto[]>> {
    return this.http.get<ApiResponse<AppealDto[]>>(`${this.basePath}/driver/${driverId}`);
  }
}
