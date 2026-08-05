package com.academy.trafficviolationsystem.core.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Base class for all search/filter objects used in paginated endpoints.
 *
 * Every domain SearchObject
 * extends this class and adds its own filter fields on top.
 *
 * The type parameter T is the ID type for the entity being searched
 */
@Getter
@Setter
public class BaseSearchObject<T> {

    // ── pagination ────────────────────────────────────────────────────────

    /** Zero-based page index. */
    private Integer page = 0;

    /** Number of records per page. Capped to 200 in BaseService to prevent abuse. */
    private Integer limit = 10;

    /**
     * When true, returns up to 1 000 records in a single page.
     * Use only for small reference data (camera list, fine rules, etc.).
     */
    private Boolean getAll;

    // ── sorting ───────────────────────────────────────────────────────────

    /**
     * Field name to sort by (must match the Java entity field name, not DB column).
     * Example: "occurredAt", "amount", "createdAt".
     * Defaults to "created" (inherited from BaseEntity) if null.
     */
    private String order;

    /**
     * Sort direction: "asc" or "desc". Defaults to "desc" when null
     * so the most recent records always appear first.
     */
    private String orderDirection;

    // ── filtering ─────────────────────────────────────────────────────────

    /**
     * Filter by a specific record ID. Useful when you need a single record
     * but want to go through the same search pipeline (e.g. to include
     * joined data that findById doesn't return).
     */
    private T id;

    /**
     * When true, the response includes the total record count matching the
     * current filters. Triggers an extra COUNT(*) query, so only set it on
     * the first page load.
     */
    private Boolean includeCount;

    /**
     * When true, soft-deleted records (deletedAt IS NOT NULL) are included
     * in results. Only officers/admins should be allowed to set this.
     * BaseService.search() checks this flag and adds the appropriate predicate.
     */
    private Boolean includeDeleted = false;
}
