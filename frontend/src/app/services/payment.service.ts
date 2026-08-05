import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApiResponse } from '../core/models/api-response.model';
import { PagedResult } from '../core/models/paged-result.model';
import { buildHttpParams } from '../core/services/http-params-builder.util';

// Adjust these import paths to match your actual model locations.
import { PaymentDto } from '../models/payment.model';
import { PaymentRequest } from '../models/payment.model';
import { PaymentSearchObject } from '../models/payment.model';

// TODO: point this at your actual API base — swap for environment.apiUrl
const API_BASE = '/api/payments';

/**
 * No update/delete — payments are immutable once submitted; only new
 * payment attempts are created. Doesn't extend BaseCrudService: unlike
 * every other domain, POST /api/payments and GET /api/payments/my both
 * return ApiResponse-wrapped bodies (confirmed against the OpenAPI spec —
 * ApiResponsePaymentDto / ApiResponsePagedResultPaymentDto), not the plain
 * DTO / PagedResult the base class expects.
 */
@Injectable({ providedIn: 'root' })
export class PaymentService {
  constructor(private readonly http: HttpClient) {}

  /** GET /api/payments (OFFICER/ADMIN) — plain PagedResult, not wrapped. */
  search(searchObject: PaymentSearchObject = {}): Observable<PagedResult<PaymentDto>> {
    const params = buildHttpParams(searchObject as unknown as Record<string, unknown>);
    return this.http.get<PagedResult<PaymentDto>>(API_BASE, { params });
  }

  /** GET /api/payments/{id} — plain DTO, not wrapped. */
  findById(id: string): Observable<PaymentDto> {
    return this.http.get<PaymentDto>(`${API_BASE}/${id}`);
  }

  /**
   * POST /api/payments — pay a fine.
   * Amount is taken from fine.totalDue server-side, never from this request.
   * Returns the transaction outcome immediately; receiptReady on the
   * response indicates whether the receipt PDF has finished generating
   * (it's async — poll findById or downloadReceipt if not yet ready).
   */
  pay(request: PaymentRequest): Observable<ApiResponse<PaymentDto>> {
    return this.http.post<ApiResponse<PaymentDto>>(API_BASE, request);
  }

  /**
   * GET /api/payments/{id}/receipt — download the payment receipt PDF.
   * Returns HTTP 404 if not yet ready — check `receiptReady` first.
   */
  downloadReceipt(id: string): Observable<Blob> {
    return this.http.get(`${API_BASE}/${id}/receipt`, { responseType: 'blob' });
  }

  /**
   * GET /api/payments/my — current citizen's own payment history.
   * Wrapped in ApiResponse<PagedResult<PaymentDto>> — confirmed against
   * the OpenAPI spec (ApiResponsePagedResultPaymentDto), unlike the plain
   * search() above.
   */
  getMyPayments(searchObject: PaymentSearchObject = {}): Observable<ApiResponse<PagedResult<PaymentDto>>> {
    const params = buildHttpParams(searchObject as unknown as Record<string, unknown>);
    return this.http.get<ApiResponse<PagedResult<PaymentDto>>>(`${API_BASE}/my`, { params });
  }

  /** GET /api/payments/fine/{fineId} (OFFICER/ADMIN) — all attempts for a fine. */
  getForFine(fineId: string): Observable<ApiResponse<PaymentDto[]>> {
    return this.http.get<ApiResponse<PaymentDto[]>>(`${API_BASE}/fine/${fineId}`);
  }
}
