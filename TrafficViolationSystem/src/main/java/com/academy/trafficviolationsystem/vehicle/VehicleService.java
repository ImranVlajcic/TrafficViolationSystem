package com.academy.trafficviolationsystem.vehicle;

import com.academy.trafficviolationsystem.audit.AuditAction;
import com.academy.trafficviolationsystem.core.exceptions.BadRequestException;
import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.core.exceptions.auth.DuplicateResourceException;
import com.academy.trafficviolationsystem.core.exceptions.vehicle.VehicleAlreadyStolenException;
import com.academy.trafficviolationsystem.core.exceptions.vehicle.VehicleDeregisteredException;
import com.academy.trafficviolationsystem.core.exceptions.vehicle.VehicleNotStolenException;
import com.academy.trafficviolationsystem.core.services.BaseCRUDService;
import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.driver.DriverRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for vehicle management.
 *
 * Implements BaseCRUDService — insert(), update(), search(), findById()
 * are handled by the base. Domain-specific methods are defined below.
 *
 * Key operations beyond CRUD:
 *   transferOwnership  — changes the registered owner, writes history row
 *   markStolen         — sets the stolen flag and can trigger notifications
 *   markFound          — clears the stolen flag
 *   findByPlate        — primary lookup used by camera detection pipeline
 *   resolveOwnerAtDate — used by ViolationService for historical disputes
 *
 * License plate normalisation:
 *   All plates are converted to uppercase before uniqueness checks and
 *   persistence so "a123bc" and "A123BC" are treated as the same plate.
 */
@Service
@Transactional
public class VehicleService implements BaseCRUDService<
        VehicleEntity, VehicleDto, VehicleSearchObject, VehicleCreateRequest, VehicleUpdateRequest, UUID> {

    private final VehicleRepository                 vehicleRepository;
    private final VehicleOwnershipHistoryRepository ownershipHistoryRepository;
    private final DriverRepository                  driverRepository;
    private final VehicleMapper                     vehicleMapper;
    private final VehicleOwnershipHistoryMapper     ownershipHistoryMapper;
    private final EntityManager                     entityManager;

    public VehicleService(VehicleRepository vehicleRepository,
                          VehicleOwnershipHistoryRepository ownershipHistoryRepository,
                          DriverRepository driverRepository,
                          VehicleMapper vehicleMapper,
                          VehicleOwnershipHistoryMapper ownershipHistoryMapper,
                          EntityManager entityManager) {
        this.vehicleRepository          = vehicleRepository;
        this.ownershipHistoryRepository = ownershipHistoryRepository;
        this.driverRepository           = driverRepository;
        this.vehicleMapper              = vehicleMapper;
        this.ownershipHistoryMapper     = ownershipHistoryMapper;
        this.entityManager              = entityManager;
    }

    // ── BaseCRUDService wiring ────────────────────────────────────────────

    @Override public VehicleRepository    getRepository()    { return vehicleRepository; }
    @Override public EntityManager        getEntityManager() { return entityManager;     }
    @Override public VehicleMapper        getMapper()        { return vehicleMapper;     }
    @Override public Class<VehicleEntity> getEntityClass()   { return VehicleEntity.class; }

    // ── lifecycle hooks ───────────────────────────────────────────────────

    @Override
    @AuditAction(value = "CREATE_VEHICLE", entityClass = VehicleEntity.class)
    public VehicleDto insert(VehicleCreateRequest request) {
        return BaseCRUDService.super.insert(request);
    }

    @Override
    @AuditAction(value = "UPDATE_VEHICLE", entityClass = VehicleEntity.class)
    public VehicleDto update(UUID id, VehicleUpdateRequest request) {
        return BaseCRUDService.super.update(id, request);
    }

    @Override
    public void beforeInsert(VehicleCreateRequest request, VehicleEntity entity) {
        // Normalise plate to uppercase
        String plate = request.getLicensePlate().toUpperCase().trim();
        entity.setLicensePlate(plate);

        if (vehicleRepository.existsByLicensePlateIgnoreCase(plate)) {
            throw new DuplicateResourceException("License plate '" + plate + "' is already registered");
        }
        if (request.getVin() != null && vehicleRepository.existsByVin(request.getVin())) {
            throw new DuplicateResourceException("VIN '" + request.getVin() + "' is already registered");
        }
        if (request.getRegistrationExpiry().isBefore(request.getRegistrationDate())) {
            throw new BadRequestException("Registration expiry must be after the registration date");
        }

        // Resolve and set the owner entity
        DriverEntity owner = driverRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new NotFoundException(
                    "Driver " + request.getOwnerId() + " not found"));
        entity.setOwner(owner);
    }

    @Override
    public void afterInsert(VehicleCreateRequest request, VehicleEntity entity) {
        // Write the first ownership history row — previousOwner is null for a new vehicle
        writeOwnershipHistory(entity, null, entity.getOwner(),
                entity.getRegistrationDate(), "Initial registration");
    }

    @Override
    public void beforeUpdate(VehicleUpdateRequest request, VehicleEntity entity) {
        if (request.getRegistrationDate() != null && request.getRegistrationExpiry() != null) {
            if (request.getRegistrationExpiry().isBefore(request.getRegistrationDate())) {
                throw new BadRequestException("Registration expiry must be after the registration date");
            }
        }
        // If only expiry is being updated, validate against persisted date
        if (request.getRegistrationExpiry() != null && request.getRegistrationDate() == null) {
            if (request.getRegistrationExpiry().isBefore(entity.getRegistrationDate())) {
                throw new BadRequestException("Registration expiry must be after the registration date");
            }
        }
    }

    // ── search filters ────────────────────────────────────────────────────

    @Override
    public List<Predicate> additionalFilter(CriteriaBuilder cb,
                                            VehicleSearchObject searchObj,
                                            Root<VehicleEntity> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (searchObj.getSearch() != null && !searchObj.getSearch().isBlank()) {
            String pattern = "%" + searchObj.getSearch().toLowerCase() + "%";
            predicates.add(cb.or(
                cb.like(cb.lower(root.get("licensePlate")), pattern),
                cb.like(cb.lower(root.get("make")),         pattern),
                cb.like(cb.lower(root.get("model")),        pattern),
                cb.like(cb.lower(root.get("vin")),          pattern)
            ));
        }

        if (searchObj.getLicensePlate() != null && !searchObj.getLicensePlate().isBlank()) {
            predicates.add(cb.equal(
                cb.lower(root.get("licensePlate")),
                searchObj.getLicensePlate().toLowerCase()
            ));
        }

        if (searchObj.getOwnerId() != null) {
            predicates.add(cb.equal(root.get("owner").get("id"), searchObj.getOwnerId()));
        }

        if (searchObj.getVehicleType() != null) {
            predicates.add(cb.equal(root.get("vehicleType"), searchObj.getVehicleType()));
        }

        if (searchObj.getFuelType() != null) {
            predicates.add(cb.equal(root.get("fuelType"), searchObj.getFuelType()));
        }

        if (searchObj.getIsStolen() != null) {
            predicates.add(cb.equal(root.get("isStolen"), searchObj.getIsStolen()));
        }

        if (searchObj.getIsActive() != null) {
            predicates.add(cb.equal(root.get("isActive"), searchObj.getIsActive()));
        }

        if (searchObj.getYear() != null) {
            predicates.add(cb.equal(root.get("year"), searchObj.getYear()));
        }

        if (searchObj.getRegistrationExpired() != null) {
            if (Boolean.TRUE.equals(searchObj.getRegistrationExpired())) {
                predicates.add(cb.lessThan(root.get("registrationExpiry"), LocalDate.now()));
            } else {
                predicates.add(cb.greaterThanOrEqualTo(root.get("registrationExpiry"), LocalDate.now()));
            }
        }

        return predicates;
    }

    // ── ownership transfer ────────────────────────────────────────────────

    /**
     * Transfers vehicle ownership to a new driver.
     *
     * Writes a VehicleOwnershipHistoryEntity row capturing the previous and
     * new owner, then updates VehicleEntity.owner. Both writes happen in the
     * same transaction — they either both succeed or both roll back.
     */
    @Transactional
    @AuditAction(value = "TRANSFER_VEHICLE_OWNERSHIP", entityClass = VehicleEntity.class)
    public VehicleDto transferOwnership(UUID vehicleId, TransferOwnershipRequest request) {
        VehicleEntity vehicle = findEntityById(vehicleId);

        if (!vehicle.isActive()) {
            throw new VehicleDeregisteredException(vehicleId);
        }

        DriverEntity newOwner = driverRepository.findById(request.getNewOwnerId())
                .orElseThrow(() -> new NotFoundException(
                    "Driver " + request.getNewOwnerId() + " not found"));

        if (newOwner.getId().equals(vehicle.getOwner().getId())) {
            throw new BadRequestException("New owner is the same as the current owner");
        }

        LocalDate transferDate = request.getTransferDate() != null
                ? request.getTransferDate()
                : LocalDate.now();

        writeOwnershipHistory(vehicle, vehicle.getOwner(), newOwner,
                transferDate, request.getNotes());

        vehicle.setOwner(newOwner);
        vehicleRepository.save(vehicle);

        return vehicleMapper.toDto(vehicle);
    }

    // ── stolen flag ───────────────────────────────────────────────────────

    /**
     * Flags a vehicle as stolen. The flag is checked by the camera detection
     * pipeline on every plate scan — matching a stolen plate raises a high-
     * priority alert regardless of whether a speed violation occurred.
     */
    @Transactional
    @AuditAction(value = "MARK_VEHICLE_STOLEN", entityClass = VehicleEntity.class)
    public VehicleDto markStolen(UUID vehicleId) {
        VehicleEntity vehicle = findEntityById(vehicleId);
        if (vehicle.isStolen()) {
            throw new VehicleAlreadyStolenException(vehicleId);
        }
        //vehicleRepository.markStolen(vehicleId);
        vehicle.setStolen(true);
        return vehicleMapper.toDto(vehicle);
    }

    /**
     * Clears the stolen flag when a vehicle is recovered.
     */
    @Transactional
    @AuditAction(value = "MARK_VEHICLE_FOUND", entityClass = VehicleEntity.class)
    public VehicleDto markFound(UUID vehicleId) {
        VehicleEntity vehicle = findEntityById(vehicleId);
        if (!vehicle.isStolen()) {
            throw new VehicleNotStolenException(vehicleId);
        }
        vehicleRepository.markFound(vehicleId);
        vehicle.setStolen(false);
        return vehicleMapper.toDto(vehicle);
    }

    // ── read helpers (used by other modules) ──────────────────────────────

    /**
     * Primary lookup for the camera detection pipeline.
     * CameraEventProcessorService calls this on every MQTT event to resolve
     * the plate to a vehicle and owner.
     */
    @Transactional(readOnly = true)
    public VehicleEntity findByPlate(String licensePlate) {
        return vehicleRepository.findByLicensePlateIgnoreCase(licensePlate.toUpperCase().trim())
                .orElseThrow(() -> new NotFoundException(
                    "No vehicle found with license plate: " + licensePlate));
    }

    /**
     * Resolves who owned a vehicle on a given date.
     * Used by ViolationService when a driver disputes a violation by claiming
     * they had already sold the vehicle before the incident date.
     */
    @Transactional(readOnly = true)
    public VehicleOwnershipHistoryDto resolveOwnerAtDate(UUID vehicleId, LocalDate date) {
        return ownershipHistoryRepository.findOwnerAtDate(vehicleId, date)
                .map(ownershipHistoryMapper::toDto)
                .orElseThrow(() -> new NotFoundException(
                    "No ownership record found for vehicle " + vehicleId + " on " + date));
    }

    /**
     * Returns the full ownership history for a vehicle.
     * Used by VehicleController GET /api/vehicles/{id}/ownership-history.
     */
    @Transactional(readOnly = true)
    public List<VehicleOwnershipHistoryDto> getOwnershipHistory(UUID vehicleId) {
        findEntityById(vehicleId); // validates vehicle exists
        return ownershipHistoryMapper.toDtoList(
            ownershipHistoryRepository.findByVehicleIdOrderByTransferDateDesc(vehicleId));
    }

    /**
     * Returns all vehicles currently flagged as stolen.
     * Used by the officer dashboard.
     */
    @Transactional(readOnly = true)
    public List<VehicleDto> getStolenVehicles() {
        return vehicleMapper.toDtoList(vehicleRepository.findByIsStolenTrueAndIsActiveTrue());
    }

    // ── private helpers ───────────────────────────────────────────────────

    private void writeOwnershipHistory(VehicleEntity vehicle,
                                        DriverEntity previousOwner,
                                        DriverEntity newOwner,
                                        LocalDate transferDate,
                                        String notes) {
        VehicleOwnershipHistoryEntity history = new VehicleOwnershipHistoryEntity();
        history.setVehicle(vehicle);
        history.setPreviousOwner(previousOwner);
        history.setNewOwner(newOwner);
        history.setTransferDate(transferDate);
        history.setNotes(notes);
        ownershipHistoryRepository.save(history);
    }
}
