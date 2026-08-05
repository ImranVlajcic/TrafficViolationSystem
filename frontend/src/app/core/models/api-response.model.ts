/**
 * Generic wrapper for single-object API responses.
 * Mirrors backend `ApiResponse<T>` (core/model/ApiResponse.java).
 *
 * Used by action endpoints that return one object rather than a page:
 *   POST /violations/{id}/confirm -> ApiResponse<ViolationDto>
 *   POST /payments                -> ApiResponse<PaymentDto>
 *   DELETE /vehicles/{id}         -> ApiResponse<void>
 *
 * Plain CRUD endpoints (GET by id, POST create, PUT update, GET search)
 * return the DTO / PagedResult directly, NOT wrapped in ApiResponse —
 * confirmed against the OpenAPI spec. Only the "action" endpoints wrap.
 */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}
