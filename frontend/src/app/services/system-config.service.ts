import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { BaseCrudService } from '../core/services/base-crud.service';
import { ApiResponse } from '../core/models/api-response.model';

// Adjust these import paths to match your actual model locations.
import { SystemConfigDto } from '../models/systemconfig.model';
import { SystemConfigUpdateRequest } from '../models/systemconfig.model';
import { SystemConfigSearchObject } from '../models/systemconfig.model';

// TODO: point this at your actual API base — swap for environment.apiUrl
const API_BASE = '/api/config';

/**
 * Note: per the OpenAPI spec, POST /api/config (create) takes the SAME
 * request body shape as PUT /api/config/{id} (SystemConfigUpdateRequest) —
 * there's no separate SystemConfigCreateRequest schema on the backend.
 * If you've defined a distinct create-request model, just swap the type
 * param below; the request shape at runtime is identical either way.
 */
@Injectable({ providedIn: 'root' })
export class SystemConfigService extends BaseCrudService<
  SystemConfigDto,
  SystemConfigUpdateRequest,
  SystemConfigUpdateRequest,
  SystemConfigSearchObject,
  number
> {
  constructor(http: HttpClient) {
    super(http, API_BASE);
  }

  /**
   * GET /api/config/category/{category} — all entries in a category.
   * Known categories: FINE, DRIVER, NOTIFICATION, PDF, MQTT.
   */
  findByCategory(category: string): Observable<ApiResponse<SystemConfigDto[]>> {
    return this.http.get<ApiResponse<SystemConfigDto[]>>(`${this.basePath}/category/${category}`);
  }
}
