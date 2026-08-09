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
import java.util.Map;
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
    // RoadZoneService.java

    @Override
    @Transactional(readOnly = true)
    public RoadZoneDto findById(Integer id) {
        return enrichWithCameraCount(getMapper().toDto(findEntityById(id)));
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

    /**
     * Overrides BaseService.search() results to enrich each DTO with cameraCount.
     * We intercept afterSearch by overriding the search method and mapping ourselves.
     */

    @Override
    public com.academy.trafficviolationsystem.core.model.PagedResult<RoadZoneDto> search(
            RoadZoneSearchObject searchObj) {
        com.academy.trafficviolationsystem.core.model.PagedResult<RoadZoneDto> result =
                BaseCRUDService.super.search(searchObj);
        List<RoadZoneDto> enriched = enrichWithCameraCounts(result.getResultList());
        return new com.academy.trafficviolationsystem.core.model.PagedResult<>(
                result.getHasMore(), enriched, result.getCount());
    }

    public List<RoadZoneDto> findActiveZones() {
        List<RoadZoneDto> dtos = zoneRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return enrichWithCameraCounts(dtos);
    }

    private RoadZoneDto enrichWithCameraCount(RoadZoneDto dto) {
        if (dto.getId() != null) {
            dto.setCameraCount(cameraRepository.countByZoneId(dto.getId()));
        }
        return dto;
    }

    private List<RoadZoneDto> enrichWithCameraCounts(List<RoadZoneDto> dtos) {
        if (dtos.isEmpty()) return dtos;

        List<Integer> zoneIds = dtos.stream()
                .map(RoadZoneDto::getId)
                .collect(Collectors.toList());

        Map<Integer, Long> counts = cameraRepository.countGroupedByZoneIds(zoneIds).stream()
                .collect(Collectors.toMap(
                        row -> (Integer) row[0],
                        row -> (Long) row[1]));

        dtos.forEach(dto -> dto.setCameraCount(
                counts.getOrDefault(dto.getId(), 0L).intValue()));

        return dtos;
    }

    private void validateSpeedLimit(Integer speedLimitKmh) {
        if (speedLimitKmh != null && speedLimitKmh <= 0) {
            throw new BadRequestException("speedLimitKmh must be greater than 0");
        }
    }

    private void validateGeoJson(String geoJson) {
        if (!StringUtils.hasText(geoJson)) return;

        com.fasterxml.jackson.databind.JsonNode root;
        try {
            root = objectMapper.readTree(geoJson);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException(
                    "geoJsonBoundary is not valid JSON: " + ex.getOriginalMessage());
        }

        com.fasterxml.jackson.databind.JsonNode typeNode = root.get("type");
        if (typeNode == null || !("Polygon".equals(typeNode.asText()) || "MultiPolygon".equals(typeNode.asText()))) {
            throw new BadRequestException(
                    "geoJsonBoundary must have type \"Polygon\" or \"MultiPolygon\"");
        }

        com.fasterxml.jackson.databind.JsonNode coords = root.get("coordinates");
        if (coords == null || !coords.isArray() || coords.isEmpty()) {
            throw new BadRequestException(
                    "geoJsonBoundary is missing a valid \"coordinates\" array");
        }

        // Polygon: coordinates = [ [ [lng,lat], [lng,lat], ... ] ]  (at least one ring, ring closed, >=4 points)
        if ("Polygon".equals(typeNode.asText())) {
            validatePolygonRings(coords);
        } else {
            // MultiPolygon: coordinates = [ Polygon, Polygon, ... ]
            for (com.fasterxml.jackson.databind.JsonNode polygon : coords) {
                validatePolygonRings(polygon);
            }
        }
    }

    private void validatePolygonRings(com.fasterxml.jackson.databind.JsonNode ringsNode) {
        if (!ringsNode.isArray() || ringsNode.isEmpty()) {
            throw new BadRequestException("geoJsonBoundary polygon must have at least one ring");
        }
        for (com.fasterxml.jackson.databind.JsonNode ring : ringsNode) {
            if (!ring.isArray() || ring.size() < 4) {
                throw new BadRequestException(
                        "geoJsonBoundary ring must have at least 4 positions (closed ring)");
            }
            com.fasterxml.jackson.databind.JsonNode first = ring.get(0);
            com.fasterxml.jackson.databind.JsonNode last = ring.get(ring.size() - 1);
            if (!first.equals(last)) {
                throw new BadRequestException(
                        "geoJsonBoundary ring must be closed (first and last position equal)");
            }
            for (com.fasterxml.jackson.databind.JsonNode position : ring) {
                if (!position.isArray() || position.size() < 2
                        || !position.get(0).isNumber() || !position.get(1).isNumber()) {
                    throw new BadRequestException(
                            "geoJsonBoundary position must be [longitude, latitude] numbers");
                }
                double lng = position.get(0).asDouble();
                double lat = position.get(1).asDouble();
                if (lng < -180 || lng > 180 || lat < -90 || lat > 90) {
                    throw new BadRequestException(
                            "geoJsonBoundary position out of range: [" + lng + ", " + lat + "]");
                }
            }
        }
    }
}
