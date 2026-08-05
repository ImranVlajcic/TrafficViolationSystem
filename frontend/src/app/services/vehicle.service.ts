import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { BaseCrudService } from '../core/services/base-crud.service';
import { ApiResponse } from '../core/models/api-response.model';

import { VehicleDto } from '../models/vehicle.model';
import { VehicleCreateRequest } from '../models/vehicle.model';
import { VehicleUpdateRequest } from '../models/vehicle.model';
import { VehicleSearchObject } from '../models/vehicle.model';
import { TransferOwnershipRequest } from '../models/vehicle.model';
import { VehicleOwnershipHistoryDto } from '../models/vehicle.model';

// TODO: point this at your actual API base — swap for environment.apiUrl
const API_BASE = '/api/vehicles';

@Injectable({ providedIn: 'root' })
export class VehicleService extends BaseCrudService<
  VehicleDto,
  VehicleCreateRequest,
  VehicleUpdateRequest,
  VehicleSearchObject,
  string
> {
  constructor(http: HttpClient) {
    super(http, API_BASE);
  }

  /** DELETE /api/vehicles/{id} — deregister, soft-delete (ADMIN only). */
  deregister(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.basePath}/${id}`);
  }

  /** POST /api/vehicles/{id}/transfer-ownership (OFFICER/ADMIN). */
  transferOwnership(id: string, request: TransferOwnershipRequest): Observable<ApiResponse<VehicleDto>> {
    return this.http.post<ApiResponse<VehicleDto>>(`${this.basePath}/${id}/transfer-ownership`, request);
  }

  /** POST /api/vehicles/{id}/mark-stolen (OFFICER/ADMIN). */
  markStolen(id: string): Observable<ApiResponse<VehicleDto>> {
    return this.http.post<ApiResponse<VehicleDto>>(`${this.basePath}/${id}/mark-stolen`, {});
  }

  /** POST /api/vehicles/{id}/mark-found (OFFICER/ADMIN). */
  markFound(id: string): Observable<ApiResponse<VehicleDto>> {
    return this.http.post<ApiResponse<VehicleDto>>(`${this.basePath}/${id}/mark-found`, {});
  }

  /** GET /api/vehicles/{id}/ownership-history */
  getOwnershipHistory(id: string): Observable<ApiResponse<VehicleOwnershipHistoryDto[]>> {
    return this.http.get<ApiResponse<VehicleOwnershipHistoryDto[]>>(`${this.basePath}/${id}/ownership-history`);
  }

  /** GET /api/vehicles/stolen — all currently stolen vehicles (OFFICER/ADMIN). */
  getStolenVehicles(): Observable<ApiResponse<VehicleDto[]>> {
    return this.http.get<ApiResponse<VehicleDto[]>>(`${this.basePath}/stolen`);
  }

  /** GET /api/vehicles/my — vehicles registered to the current citizen. */
  getMyVehicles(): Observable<ApiResponse<VehicleDto[]>> {
    return this.http.get<ApiResponse<VehicleDto[]>>(`${this.basePath}/my`);
  }
}
