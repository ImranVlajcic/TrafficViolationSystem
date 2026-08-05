import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { BaseCrudService } from '../core/services/base-crud.service';
import { ApiResponse } from '../core/models/api-response.model';

// Adjust these import paths to match your actual model locations.
import { CameraDto } from '../models/camera.model';
import { CameraCreateRequest } from '../models/camera.model';
import { CameraUpdateRequest } from '../models/camera.model';
import { CameraSearchObject } from '../models/camera.model';
import { LogMaintenanceRequest } from '../models/camera.model';
import { CameraMaintenanceLogDto } from '../models/camera.model';
import { CameraEventDto } from '../models/camera.model';

// TODO: point this at your actual API base — swap for environment.apiUrl
const API_BASE = '/api/cameras';

@Injectable({ providedIn: 'root' })
export class CameraService extends BaseCrudService<
  CameraDto,
  CameraCreateRequest,
  CameraUpdateRequest,
  CameraSearchObject,
  number
> {
  constructor(http: HttpClient) {
    super(http, API_BASE);
  }

  /** DELETE /api/cameras/{id} — decommission, soft-delete (ADMIN only). */
  decommission(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.basePath}/${id}`);
  }

  /** GET /api/cameras/{id}/maintenance — maintenance history for a camera. */
  getMaintenanceHistory(id: number): Observable<ApiResponse<CameraMaintenanceLogDto[]>> {
    return this.http.get<ApiResponse<CameraMaintenanceLogDto[]>>(`${this.basePath}/${id}/maintenance`);
  }

  /** POST /api/cameras/{id}/maintenance — log a maintenance visit (ADMIN only). */
  logMaintenance(id: number, request: LogMaintenanceRequest): Observable<ApiResponse<CameraMaintenanceLogDto>> {
    return this.http.post<ApiResponse<CameraMaintenanceLogDto>>(`${this.basePath}/${id}/maintenance`, request);
  }

  /** POST /api/cameras/{id}/maintenance/{logId}/complete — mark scheduled entry completed (ADMIN only). */
  completeMaintenance(id: number, logId: string): Observable<ApiResponse<CameraMaintenanceLogDto>> {
    return this.http.post<ApiResponse<CameraMaintenanceLogDto>>(
      `${this.basePath}/${id}/maintenance/${logId}/complete`,
      {}
    );
  }

  /** GET /api/cameras/{id}/events — raw MQTT event history for a camera, newest first. */
  getEvents(id: number): Observable<ApiResponse<CameraEventDto[]>> {
    return this.http.get<ApiResponse<CameraEventDto[]>>(`${this.basePath}/${id}/events`);
  }

  /** GET /api/cameras/offline — all currently offline active cameras. */
  getOffline(): Observable<ApiResponse<CameraDto[]>> {
    return this.http.get<ApiResponse<CameraDto[]>>(`${this.basePath}/offline`);
  }
}
