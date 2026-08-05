package com.academy.trafficviolationsystem.core.config.seed;

import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.vehicle.FuelType;
import com.academy.trafficviolationsystem.vehicle.VehicleEntity;
import com.academy.trafficviolationsystem.vehicle.VehicleRepository;
import com.academy.trafficviolationsystem.vehicle.VehicleType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seeds ~130 vehicles distributed across the seeded drivers (some drivers
 * own more than one vehicle, some own none).
 */
@Component
public class VehicleSeeder {

    private static final int VEHICLE_COUNT = 130;

    private final VehicleRepository vehicleRepository;

    public VehicleSeeder(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public List<VehicleEntity> seed(List<DriverEntity> drivers) {
        if (vehicleRepository.count() > 0) {
            return vehicleRepository.findAll();
        }

        List<VehicleEntity> vehicles = new ArrayList<>();
        Set<String> usedPlates = new HashSet<>();
        int stolenAssigned = 0;

        for (int i = 0; i < VEHICLE_COUNT; i++) {
            DriverEntity owner = SeedRandom.pick(drivers);

            String plate;
            do {
                plate = SeedRandom.pick(SeedConstants.PLATE_CITY_CODES) + "-"
                        + SeedRandom.letters(1) + "-" + SeedRandom.digits(3);
            } while (!usedPlates.add(plate));

            boolean electric = SeedRandom.chance(0.08);
            VehicleType vehicleType = weightedVehicleType();

            LocalDate registrationDate = SeedRandom.pastDate(60, 365 * 6);
            // ~15% already expired, to exercise the registration-expiry job/frontend states.
            LocalDate registrationExpiry = SeedRandom.chance(0.15)
                    ? LocalDate.now().minusDays(SeedRandom.intBetween(1, 120))
                    : registrationDate.plusYears(1).plusDays(SeedRandom.intBetween(0, 300));

            VehicleEntity vehicle = new VehicleEntity();
            vehicle.setLicensePlate(plate);
            vehicle.setVin(SeedRandom.chance(0.7) ? randomVin() : null);
            vehicle.setMake(SeedRandom.pick(SeedConstants.VEHICLE_MAKES));
            vehicle.setModel(SeedRandom.pick(SeedConstants.VEHICLE_MODELS));
            vehicle.setYear(SeedRandom.intBetween(2005, 2025));
            vehicle.setColor(SeedRandom.pick(SeedConstants.VEHICLE_COLORS));
            vehicle.setVehicleType(vehicleType);
            vehicle.setEngineCc(electric ? null : SeedRandom.intBetween(1000, 3000));
            vehicle.setFuelType(electric ? FuelType.ELECTRIC : weightedFuelType());
            vehicle.setRegistrationDate(registrationDate);
            vehicle.setRegistrationExpiry(registrationExpiry);
            // Only 2 vehicles ever marked stolen — enough to test the alert path
            // without making every camera hit trigger one.
            vehicle.setStolen(stolenAssigned < 2 && SeedRandom.chance(0.02));
            if (vehicle.isStolen()) stolenAssigned++;
            vehicle.setActive(SeedRandom.chance(0.96));
            vehicle.setOwner(owner);

            vehicles.add(vehicleRepository.save(vehicle));
        }

        return vehicles;
    }

    private VehicleType weightedVehicleType() {
        double r = SeedRandom.RNG.nextDouble();
        if (r < 0.70) return VehicleType.CAR;
        if (r < 0.80) return VehicleType.VAN;
        if (r < 0.88) return VehicleType.MOTORCYCLE;
        if (r < 0.94) return VehicleType.TRUCK;
        if (r < 0.98) return VehicleType.BUS;
        return VehicleType.TRACTOR;
    }

    private FuelType weightedFuelType() {
        double r = SeedRandom.RNG.nextDouble();
        if (r < 0.45) return FuelType.DIESEL;
        if (r < 0.85) return FuelType.GASOLINE;
        if (r < 0.92) return FuelType.LPG;
        if (r < 0.97) return FuelType.HYBRID;
        return FuelType.CNG;
    }

    private String randomVin() {
        // 17-char VIN-shaped string; not a validly checksummed VIN, just plausible-looking.
        return SeedRandom.letters(3) + SeedRandom.digits(6) + SeedRandom.letters(2) + SeedRandom.digits(6);
    }
}