import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { BaseCrudService } from '../core/services/base-crud.service';
import { ApiResponse } from '../core/models/api-response.model';

// Adjust these import paths to match your actual model locations.
import { RoadZoneDto } from '../models/roadzone.model';
import { RoadZoneCreateRequest } from '../models/roadzone.model';
import { RoadZoneUpdateRequest } from '../models/roadzone.model';
import { RoadZoneSearchObject } from '../models/roadzone.model';

// TODO: point this at your actual API base — swap for environment.apiUrl
const API_BASE = '/api/zones';

@Injectable({ providedIn: 'root' })
export class RoadZoneService extends BaseCrudService<
  RoadZoneDto,
  RoadZoneCreateRequest,
  RoadZoneUpdateRequest,
  RoadZoneSearchObject,
  number
> {
  constructor(http: HttpClient) {
    super(http, API_BASE);
  }

  /** DELETE /api/zones/{id} — soft-delete, clears camera assignments (ADMIN only). */
  deleteZone(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.basePath}/${id}`);
  }

  /** POST /api/zones/{id}/assign-camera/{cameraId} (ADMIN only). */
  assignCamera(id: number, cameraId: number): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.basePath}/${id}/assign-camera/${cameraId}`, {});
  }

  /** DELETE /api/zones/unassign-camera/{cameraId} (ADMIN only). */
  unassignCamera(cameraId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.basePath}/unassign-camera/${cameraId}`);
  }

  /** GET /api/zones/active — all active zones, for map layers and camera-assignment dropdowns. */
  findActiveZones(): Observable<ApiResponse<RoadZoneDto[]>> {
    return this.http.get<ApiResponse<RoadZoneDto[]>>(`${this.basePath}/active`);
  }
}
