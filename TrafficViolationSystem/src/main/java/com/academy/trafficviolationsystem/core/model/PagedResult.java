package com.academy.trafficviolationsystem.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Standard wrapper for all paginated list responses.
 *
 * Returned by every GET / (search) endpoint via BaseController.
 * The frontend reads hasMore to decide whether to show a "Load more" button,
 * and count (when requested) to render total-result pagination controls.
 *
 * How hasMore works:
 *   BaseService.search() fetches (limit + 1) rows. If it gets (limit + 1)
 *   back, there is at least one more page, so hasMore = true and the extra
 *   row is stripped before returning. This avoids a separate COUNT query
 *   on every request.
 *
 * count:
 *   Only populated when the caller sets includeCount = true on the search
 *   object. This triggers a separate COUNT(*) query. Only do this on the
 *   first page load, not on every subsequent page.
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagedResult<T> {

    /** True when there are more records beyond this page. */
    private Boolean hasMore;

    /** The records for this page. */
    private List<T> resultList;

    /**
     * Total matching record count across all pages.
     * Null unless the caller set includeCount = true on the search object.
     */
    private Long count;
}
