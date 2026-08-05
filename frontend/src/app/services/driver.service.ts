import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { BaseCrudService } from '../core/services/base-crud.service';
import { ApiResponse } from '../core/models/api-response.model';

// Adjust these import paths to match your actual model locations.
import { DriverDto } from '../models/driver.model';
import { DriverCreateRequest } from '../models/driver.model';
import { DriverUpdateRequest } from '../models/driver.model';
import { DriverSearchObject } from '../models/driver.model';
import { SuspendDriverRequest } from '../models/driver.model';
import { LicenseSuspensionDto } from '../models/driver.model';
import { DriverPointHistoryDto } from '../models/driver.model';

// TODO: point this at your actual API base — swap for environment.apiUrl
const API_BASE = '/api/drivers';

@Injectable({ providedIn: 'root' })
export class DriverService extends BaseCrudService<
  DriverDto,
  DriverCreateRequest,
  DriverUpdateRequest,
  DriverSearchObject,
  string
> {
  constructor(http: HttpClient) {
    super(http, API_BASE);
  }

  /**
   * POST /api/drivers/{id}/suspend (OFFICER/ADMIN).
   * Suspends the driver's license, returns the created suspension record.
   */
  suspend(id: string, request: SuspendDriverRequest): Observable<ApiResponse<LicenseSuspensionDto>> {
    return this.http.post<ApiResponse<LicenseSuspensionDto>>(`${this.basePath}/${id}/suspend`, request);
  }

  /**
   * POST /api/drivers/{id}/lift-suspension (OFFICER/ADMIN).
   * Lifts the driver's current suspension.
   */
  liftSuspension(id: string): Observable<ApiResponse<DriverDto>> {
    return this.http.post<ApiResponse<DriverDto>>(`${this.basePath}/${id}/lift-suspension`, {});
  }

  /**
   * POST /api/drivers/{id}/link-user?userId={userId} (ADMIN).
   * Links a citizen user account to this driver record.
   */
  linkUserAccount(id: string, userId: string): Observable<ApiResponse<DriverDto>> {
    return this.http.post<ApiResponse<DriverDto>>(`${this.basePath}/${id}/link-user`, null, {
      params: { userId },
    });
  }

  /** GET /api/drivers/{id}/suspensions (OFFICER/ADMIN) — full suspension history. */
  getSuspensionHistory(id: string): Observable<ApiResponse<LicenseSuspensionDto[]>> {
    return this.http.get<ApiResponse<LicenseSuspensionDto[]>>(`${this.basePath}/${id}/suspensions`);
  }

  /** GET /api/drivers/{id}/points (OFFICER/ADMIN) — penalty point history. */
  getPointHistory(id: string): Observable<ApiResponse<DriverPointHistoryDto[]>> {
    return this.http.get<ApiResponse<DriverPointHistoryDto[]>>(`${this.basePath}/${id}/points`);
  }

  /** GET /api/drivers/by-license/{licenseNumber} (OFFICER/ADMIN). */
  findByLicenseNumber(licenseNumber: string): Observable<ApiResponse<DriverDto>> {
    return this.http.get<ApiResponse<DriverDto>>(`${this.basePath}/by-license/${licenseNumber}`);
  }
}
