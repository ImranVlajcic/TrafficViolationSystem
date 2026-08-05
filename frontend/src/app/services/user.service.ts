import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { BaseCrudService } from '../core/services/base-crud.service';
import { ApiResponse } from '../core/models/api-response.model';

import { UserDto } from '../models/user.model';
import { UserCreateRequest } from '../models/user.model';
import { UserUpdateRequest } from '../models/user.model';
import { UserSearchObject } from '../models/user.model';
import { ChangePasswordRequest } from '../models/user.model';

// TODO: point this at your actual API base — swap for environment.apiUrl
const API_BASE = '/api/users';

@Injectable({ providedIn: 'root' })
export class UserService extends BaseCrudService<
  UserDto,
  UserCreateRequest,
  UserUpdateRequest,
  UserSearchObject,
  string
> {
  constructor(http: HttpClient) {
    super(http, API_BASE);
  }

  /** DELETE /api/users/{id} — soft-delete (ADMIN only). */
  deleteUser(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.basePath}/${id}`);
  }

  /** POST /api/users/{id}/change-password */
  changePassword(id: string, request: ChangePasswordRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.basePath}/${id}/change-password`, request);
  }

  /** GET /api/users/me — currently authenticated user's own profile. */
  getProfile(): Observable<ApiResponse<UserDto>> {
    return this.http.get<ApiResponse<UserDto>>(`${this.basePath}/me`);
  }
}
