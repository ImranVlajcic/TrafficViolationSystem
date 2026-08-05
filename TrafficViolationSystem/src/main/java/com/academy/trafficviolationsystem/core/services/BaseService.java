package com.academy.trafficviolationsystem.core.services;

import com.academy.trafficviolationsystem.core.entities.AbstractEntity;
import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.core.mappers.BaseMapper;
import com.academy.trafficviolationsystem.core.model.BaseSearchObject;
import com.academy.trafficviolationsystem.core.model.PagedResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.repository.CrudRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Core read + search contract that every domain service inherits.
 *
 * Soft-delete filter — records where deletedAt IS NOT NULL are excluded
 * from all searches unless the caller sets includeDeleted = true on the
 * search object.  This makes soft-delete actually work; without this the
 * example's @PreRemove hook sets deletedAt but the query never checked it.
 *
 */
public interface BaseService<E extends AbstractEntity, DTO, SObj extends BaseSearchObject<?>, T> {

    CrudRepository<E, T> getRepository();

    EntityManager getEntityManager();

    BaseMapper<E, DTO> getMapper();

    Class<E> getEntityClass();

    // ── single-record helpers ─────────────────────────────────────────────

    default E findEntityById(T id) {
        return getRepository().findById(id)
                .orElseThrow(() -> new NotFoundException("Entity with id " + id + " not found"));
    }

    default DTO findById(T id) {
        return getMapper().toDto(findEntityById(id));
    }

    // ── paginated search ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    default PagedResult<DTO> search(SObj searchObj) {

        // ── pagination ────────────────────────────────────────────────────
        int page  = searchObj.getPage()  != null ? searchObj.getPage()  : 0;
        int limit = searchObj.getLimit() != null ? searchObj.getLimit() : 10;

        if (Boolean.TRUE.equals(searchObj.getGetAll())) {
            limit = 1000;
        }

        // Hard cap – prevents clients from accidentally requesting huge pages.
        limit = Math.min(limit, 200);

        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // ── main data query ───────────────────────────────────────────────
        CriteriaQuery<E> query = cb.createQuery(getEntityClass());
        Root<E> root = query.from(getEntityClass());

        List<Predicate> predicates = new ArrayList<>();

        // 1. Soft-delete filter (the fix that makes AbstractEntity.deletedAt useful)
        if (!Boolean.TRUE.equals(searchObj.getIncludeDeleted())) {
            predicates.add(cb.isNull(root.get("deletedAt")));
        }

        // 2. Domain-specific filters contributed by each service implementation
        predicates.addAll(additionalFilter(cb, searchObj, root));

        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(new Predicate[0]));
        }

        // 3. Ordering — wire up the 'order' / 'orderDirection' fields
        applyOrdering(cb, query, root, searchObj);

        List<E> results = em.createQuery(query)
                .setFirstResult(page * limit)
                .setMaxResults(limit + 1)   // fetch one extra to determine hasMore
                .getResultList();

        boolean hasMore = results.size() > limit;
        if (hasMore) {
            results = results.subList(0, limit);
        }

        List<DTO> dtoList = getMapper().toDtoList(results);

        // ── optional count query ──────────────────────────────────────────
        Long count = null;
        if (Boolean.TRUE.equals(searchObj.getIncludeCount())) {
            CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
            Root<E> countRoot = countQuery.from(getEntityClass());
            countQuery.select(cb.count(countRoot));

            List<Predicate> countPredicates = new ArrayList<>();

            if (!Boolean.TRUE.equals(searchObj.getIncludeDeleted())) {
                countPredicates.add(cb.isNull(countRoot.get("deletedAt")));
            }
            countPredicates.addAll(additionalFilter(cb, searchObj, countRoot));

            if (!countPredicates.isEmpty()) {
                countQuery.where(countPredicates.toArray(new Predicate[0]));
            }

            count = em.createQuery(countQuery).getSingleResult();
        }

        return new PagedResult<>(hasMore, dtoList, count);
    }

    /**
     * Override in each service to add domain-specific WHERE predicates.
     */
    default List<Predicate> additionalFilter(CriteriaBuilder cb, SObj searchObj, Root<E> root) {
        return new ArrayList<>();
    }

    // ── private helper ────────────────────────────────────────────────────

    /**
     * Applies ORDER BY to the query.
     *
     * The field name must match the Java entity field, not the DB column.
     * Invalid field names will throw an IllegalArgumentException from the
     * JPA criteria API at runtime, which the GlobalExceptionHandler will catch.
     */
    private void applyOrdering(CriteriaBuilder cb, CriteriaQuery<E> query,
                                Root<E> root, SObj searchObj) {
        String fieldName = searchObj.getOrder();
        String direction = searchObj.getOrderDirection();

        if (fieldName == null || fieldName.isBlank()) {
            // Default: newest records first
            query.orderBy(cb.desc(root.get("created")));
            return;
        }

        Order order = "asc".equalsIgnoreCase(direction)
                ? cb.asc(root.get(fieldName))
                : cb.desc(root.get(fieldName));

        query.orderBy(order);
    }
}
