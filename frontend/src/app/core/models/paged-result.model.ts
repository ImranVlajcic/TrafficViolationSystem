/**
 * Generic wrapper for all paginated list responses.
 * Mirrors backend `PagedResult<T>` (core/model/PagedResult.java).
 *
 * hasMore: true when there are more records beyond this page
 *   (backend fetches limit+1 rows to detect this without a COUNT query).
 * count: total matching record count across all pages — only populated
 *   when the search request set includeCount = true.
 */
export interface PagedResult<T> {
  hasMore: boolean;
  resultList: T[];
  count: number | null;
}
