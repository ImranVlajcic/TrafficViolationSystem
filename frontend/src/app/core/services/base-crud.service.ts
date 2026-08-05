import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { PagedResult } from '../models/paged-result.model';
import { BaseSearchObject } from '../models/base-search-object.model';
import { buildHttpParams } from './http-params-builder.util';

/**
 * Thin, domain-agnostic wrapper around HttpClient mirroring the backend's
 * BaseController / BaseCRUDController abstraction (core module, 1.3).
 *
 * Covers the four "standard" operations every domain controller implements:
 *   GET    {basePath}          -> search   (paginated)
 *   GET    {basePath}/{id}     -> findById
 *   POST   {basePath}          -> create
 *   PUT    {basePath}/{id}     -> update
 *
 * Delete is intentionally NOT included here: not every domain exposes it
 * (Violation has no delete endpoint at all — status transitions are used
 * instead), and where it does exist it returns ApiResponse<void>, not a DTO.
 * Add `delete()` in the concrete service when the domain supports it.
 *
 * TDto        - read DTO returned by the API (e.g. VehicleDto)
 * TCreateReq  - request body for POST (e.g. VehicleCreateRequest)
 * TUpdateReq  - request body for PUT (e.g. VehicleUpdateRequest)
 * TSearch     - search object for GET, extending BaseSearchObject
 * TId         - 'string' (UUID) or 'number' (int) primary key type
 */
export abstract class BaseCrudService<
  TDto,
  TCreateReq,
  TUpdateReq,
  TSearch extends BaseSearchObject<string | number>,
  TId extends string | number = string
> {
  protected constructor(
    protected readonly http: HttpClient,
    protected readonly basePath: string
  ) {}

  search(searchObject: TSearch = {} as TSearch): Observable<PagedResult<TDto>> {
    const params = buildHttpParams(searchObject as unknown as Record<string, unknown>);
    return this.http.get<PagedResult<TDto>>(this.basePath, { params });
  }

  findById(id: TId): Observable<TDto> {
    return this.http.get<TDto>(`${this.basePath}/${id}`);
  }

  create(request: TCreateReq): Observable<TDto> {
    return this.http.post<TDto>(this.basePath, request);
  }

  update(id: TId, request: TUpdateReq): Observable<TDto> {
    return this.http.put<TDto>(`${this.basePath}/${id}`, request);
  }
}
