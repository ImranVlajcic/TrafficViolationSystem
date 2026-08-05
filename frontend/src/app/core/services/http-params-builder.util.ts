import { HttpParams } from '@angular/common/http';

/**
 * Converts a flat search-object (BaseSearchObject + domain filters) into
 * HttpParams for a GET request. Skips undefined/null/empty-string fields
 * so we never send e.g. `status=` to the backend.
 *
 * Dates should already be plain 'YYYY-MM-DD' strings on the search object
 * (LocalDate on the backend) — don't pass Date objects in here.
 */
export function buildHttpParams(searchObject: Record<string, unknown>): HttpParams {
  let params = new HttpParams();

  for (const [key, value] of Object.entries(searchObject)) {
    if (value === undefined || value === null || value === '') {
      continue;
    }
    params = params.set(key, String(value));
  }

  return params;
}
