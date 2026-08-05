package com.academy.trafficviolationsystem.rodezone;

import com.academy.trafficviolationsystem.audit.AuditAction;
import com.academy.trafficviolationsystem.camera.CameraEntity;
import com.academy.trafficviolationsystem.camera.CameraRepository;
import com.academy.trafficviolationsystem.core.exceptions.BadRequestException;
import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.core.services.BaseCRUDService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for road zone management.
 *
 * Implements BaseCRUDService — wired through the core BaseService/BaseCRUDService
 * pattern using EntityManager + Criteria API, exactly like every other module.
 *
 * insert(), update(), search(), findById() are all handled by the base.
 * Domain-specific logic lives in the lifecycle hooks and extra methods below.
 *
 * Camera assignment:
 *   Zones do not have a @OneToMany to cameras — CameraEntity.zoneId is a raw
 *   Integer FK column to avoid a circular dependency (camera/ depends on zone/,
 *   not vice versa). Assignment is done via CameraRepository @Modifying queries.
 *
 * cameraCount in DTO:
 *   Populated after mapping via CameraRepository.countByZoneId() in afterToDto().
 *   The mapper leaves cameraCount = null and the service fills it in.
 */
@Service
@Transactional
public class RoadZoneService implements BaseCRUDService<
        RoadZoneEntity, RoadZoneDto,
        RoadZoneSearchObject,
        RoadZoneCreateRequest, RoadZoneUpdateRequest,
        Integer> {

    private final RoadZoneRepository zoneRepository;
    private final CameraRepository   cameraRepository;
    private final RoadZoneMapper     mapper;
    private final ObjectMapper       objectMapper;
    private final EntityManager      entityManager;

    public RoadZoneService(RoadZoneRepository zoneRepository,
                            CameraRepository cameraRepository,
                            RoadZoneMapper mapper,
                            ObjectMapper objectMapper,
                            EntityManager entityManager) {
        this.zoneRepository  = zoneRepository;
        this.cameraRepository = cameraRepository;
        this.mapper          = mapper;
        this.objectMapper    = objectMapper;
        this.entityManager   = entityManager;
    }

    // ── BaseCRUDService wiring ────────────────────────────────────────────────

    @Override public RoadZoneRepository    getRepository()    { return zoneRepository; }
    @Override public EntityManager         getEntityManager() { return entityManager;  }
    @Override public RoadZoneMapper        getMapper()        { return mapper;         }
    @Override public Class<RoadZoneEntity> getEntityClass()   { return RoadZoneEntity.class; }

    // ── lifecycle hooks ───────────────────────────────────────────────────────

    @Override
    @AuditAction(value = "CREATE_ROAD_ZONE", entityClass = RoadZoneEntity.class)
    public RoadZoneDto insert(RoadZoneCreateRequest request) {
        return BaseCRUDService.super.insert(request);
    }

    @Override
    @AuditAction(value = "UPDATE_ROAD_ZONE", entityClass = RoadZoneEntity.class)
    public RoadZoneDto update(Integer id, RoadZoneUpdateRequest request) {
        return BaseCRUDService.super.update(id, request);
    }

    @Override
    public void beforeInsert(RoadZoneCreateRequest request, RoadZoneEntity entity) {
        validateSpeedLimit(request.getSpeedLimitKmh());
        validateGeoJson(request.getGeoJsonBoundary());
    }

    @Override
    public void beforeUpdate(RoadZoneUpdateRequest request, RoadZoneEntity entity) {
        if (request.getSpeedLimitKmh() != null) {
            validateSpeedLimit(request.getSpeedLimitKmh());
        }
        validateGeoJson(request.getGeoJsonBoundary());
    }

    // ── delete with camera cleanup ──────────────────────────────────────────────

    /**
     * Soft-deletes a zone via @PreRemove (sets deletedAt on the entity).
     * Clears the zoneId FK on all cameras assigned to this zone first
     * so no camera is left pointing at a deleted zone.
     * Call this instead of the base delete() to ensure camera cleanup runs.
     */
    @Transactional
    @AuditAction(value = "DELETE_ROAD_ZONE", entityClass = RoadZoneEntity.class)
    public void deleteZone(Integer id) {
        RoadZoneEntity zone = findEntityById(id);
        cameraRepository.clearZoneId(id);
        getRepository().delete(zone); // triggers @PreRemove -> sets deletedAt
    }

    // ── search filters ────────────────────────────────────────────────────────

    @Override
    public List<Predicate> additionalFilter(CriteriaBuilder cb,
                                             RoadZoneSearchObject searchObj,
                                             Root<RoadZoneEntity> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (StringUtils.hasText(searchObj.getSearch())) {
            String pattern = "%" + searchObj.getSearch().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("name")), pattern));
        }
        if (searchObj.getZoneType() != null) {
            predicates.add(cb.equal(root.get("zoneType"), searchObj.getZoneType()));
        }
        if (searchObj.getIsActive() != null) {
            predicates.add(cb.equal(root.get("isActive"), searchObj.getIsActive()));
        }
        return predicates;
    }

    // ── DTO enrichment ────────────────────────────────────────────────────────

    /**
     * Overrides BaseService.findById() to enrich the DTO with cameraCount.
     * The mapper leaves cameraCount = null; we fill it here.
     */
    @Override
    @Transactional(readOnly = true)
    public RoadZoneDto findById(Integer id) {
        return enrichWithCameraCount(getMapper().toDto(findEntityById(id)));
    }

    /**
     * Overrides BaseService.search() results to enrich each DTO with cameraCount.
     * We intercept afterSearch by overriding the search method and mapping ourselves.
     */
    @Override
    public com.academy.trafficviolationsystem.core.model.PagedResult<RoadZoneDto> search(
            RoadZoneSearchObject searchObj) {
        com.academy.trafficviolationsystem.core.model.PagedResult<RoadZoneDto> result =
                BaseCRUDService.super.search(searchObj);
        // Enrich each DTO with cameraCount
        List<RoadZoneDto> enriched = result.getResultList()
                .stream()
                .map(this::enrichWithCameraCount)
                .collect(Collectors.toList());
        return new com.academy.trafficviolationsystem.core.model.PagedResult<>(
                result.getHasMore(), enriched, result.getCount());
    }

    // ── camera assignment ─────────────────────────────────────────────────────

    /**
     * Assigns a camera to this zone by updating CameraEntity.zoneId.
     * Validates that both zone and camera exist before updating.
     */
    @Transactional
    @AuditAction(value = "ASSIGN_CAMERA_TO_ZONE", entityClass = RoadZoneEntity.class)
    public void assignCameraToZone(Integer zoneId, Integer cameraId) {
        if (!zoneRepository.existsById(zoneId)) {
            throw new NotFoundException("RoadZone " + zoneId + " not found");
        }
        cameraRepository.findById(cameraId)
                .orElseThrow(() -> new NotFoundException("Camera " + cameraId + " not found"));
        cameraRepository.updateZoneId(cameraId, zoneId);
    }

    /** Removes a camera from its current zone (sets zoneId = null). */
    @Transactional
    @AuditAction(value = "UNASSIGN_CAMERA_FROM_ZONE", entityClass = CameraEntity.class)
    public void unassignCameraFromZone(Integer cameraId) {
        cameraRepository.findById(cameraId)
                .orElseThrow(() -> new NotFoundException("Camera " + cameraId + " not found"));
        cameraRepository.updateZoneId(cameraId, null);
    }

    /** Active zones for map layer / dropdown — no pagination needed. */
    @Transactional(readOnly = true)
    public List<RoadZoneDto> findActiveZones() {
        return zoneRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(e -> enrichWithCameraCount(mapper.toDto(e)))
                .collect(Collectors.toList());
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private RoadZoneDto enrichWithCameraCount(RoadZoneDto dto) {
        if (dto.getId() != null) {
            dto.setCameraCount(cameraRepository.countByZoneId(dto.getId()));
        }
        return dto;
    }

    private void validateSpeedLimit(Integer speedLimitKmh) {
        if (speedLimitKmh != null && speedLimitKmh <= 0) {
            throw new BadRequestException("speedLimitKmh must be greater than 0");
        }
    }

    private void validateGeoJson(String geoJson) {
        if (!StringUtils.hasText(geoJson)) return;
        try {
            objectMapper.readTree(geoJson);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException(
                "geoJsonBoundary is not valid JSON: " + ex.getOriginalMessage());
        }
    }
}
