import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { BaseCrudService } from '../core/services/base-crud.service';
import { ApiResponse } from '../core/models/api-response.model';

import { ViolationDto } from '../models/violation.model';
import { ViolationCreateRequest } from '../models/violation.model';
import { ViolationUpdateRequest } from '../models/violation.model';
import { ViolationSearchObject } from '../models/violation.model';
import { ReviewViolationRequest } from '../models/violation.model';

// TODO: point this at your actual API base — swap for environment.apiUrl
const API_BASE = '/api/violations';

@Injectable({ providedIn: 'root' })
export class ViolationService extends BaseCrudService<
  ViolationDto,
  ViolationCreateRequest,
  ViolationUpdateRequest,
  ViolationSearchObject,
  string
> {
  constructor(http: HttpClient) {
    super(http, API_BASE);
  }

  /**
   * POST /api/violations/{id}/confirm (OFFICER/ADMIN).
   * Transitions PENDING -> CONFIRMED and triggers fine issuance.
   */
  confirm(id: string, request: ReviewViolationRequest): Observable<ApiResponse<ViolationDto>> {
    return this.http.post<ApiResponse<ViolationDto>>(`${this.basePath}/${id}/confirm`, request);
  }

  /**
   * POST /api/violations/{id}/dismiss (OFFICER/ADMIN).
   * Transitions PENDING or CONFIRMED -> DISMISSED. No fine is issued.
   */
  dismiss(id: string, request: ReviewViolationRequest): Observable<ApiResponse<ViolationDto>> {
    return this.http.post<ApiResponse<ViolationDto>>(`${this.basePath}/${id}/dismiss`, request);
  }

  /** GET /api/violations/vehicle/{vehicleId} (OFFICER/ADMIN). */
  getForVehicle(vehicleId: string): Observable<ApiResponse<ViolationDto[]>> {
    return this.http.get<ApiResponse<ViolationDto[]>>(`${this.basePath}/vehicle/${vehicleId}`);
  }

  /** GET /api/violations/driver/{driverId}. */
  getForDriver(driverId: string): Observable<ApiResponse<ViolationDto[]>> {
    return this.http.get<ApiResponse<ViolationDto[]>>(`${this.basePath}/driver/${driverId}`);
  }

  /** GET /api/violations/pending (OFFICER/ADMIN) — all violations awaiting review. */
  getPendingReview(): Observable<ApiResponse<ViolationDto[]>> {
    return this.http.get<ApiResponse<ViolationDto[]>>(`${this.basePath}/pending`);
  }

  /** GET /api/violations/my — violations belonging to the current citizen. */
  getMyViolations(): Observable<ApiResponse<ViolationDto[]>> {
    return this.http.get<ApiResponse<ViolationDto[]>>(`${this.basePath}/my`);
  }
}
