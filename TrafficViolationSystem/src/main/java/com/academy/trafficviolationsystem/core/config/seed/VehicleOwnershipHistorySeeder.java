package com.academy.trafficviolationsystem.core.config.seed;

import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.vehicle.VehicleEntity;
import com.academy.trafficviolationsystem.vehicle.VehicleOwnershipHistoryEntity;
import com.academy.trafficviolationsystem.vehicle.VehicleOwnershipHistoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds an ownership trail for every vehicle: one initial-registration row
 * for all vehicles, plus a second "transferred to current owner" row for
 * ~20 vehicles — enough to exercise the appeal-evidence lookup path
 * ("I had already sold the car") without needing every vehicle to have a
 * multi-owner history.
 */
@Component
public class VehicleOwnershipHistorySeeder {

    private final VehicleOwnershipHistoryRepository ownershipHistoryRepository;

    public VehicleOwnershipHistorySeeder(VehicleOwnershipHistoryRepository ownershipHistoryRepository) {
        this.ownershipHistoryRepository = ownershipHistoryRepository;
    }

    public void seed(List<VehicleEntity> vehicles, List<DriverEntity> drivers) {
        if (ownershipHistoryRepository.count() > 0) return;

        int transfersAdded = 0;
        for (VehicleEntity vehicle : vehicles) {
            boolean withTransfer = transfersAdded < 20 && SeedRandom.chance(0.2);

            if (withTransfer) {
                DriverEntity previousOwner = SeedRandom.pick(drivers);
                if (previousOwner.getId().equals(vehicle.getOwner().getId())) {
                    withTransfer = false;
                } else {
                    save(vehicle, null, previousOwner, vehicle.getRegistrationDate());
                    save(vehicle, previousOwner, vehicle.getOwner(),
                            vehicle.getRegistrationDate().plusMonths(SeedRandom.intBetween(2, 30)));
                    transfersAdded++;
                }
            }

            if (!withTransfer) {
                save(vehicle, null, vehicle.getOwner(), vehicle.getRegistrationDate());
            }
        }
    }

    private void save(VehicleEntity vehicle, DriverEntity previousOwner,
                      DriverEntity newOwner, java.time.LocalDate transferDate) {
        VehicleOwnershipHistoryEntity history = new VehicleOwnershipHistoryEntity();
        history.setVehicle(vehicle);
        history.setPreviousOwner(previousOwner);
        history.setNewOwner(newOwner);
        history.setTransferDate(transferDate);
        history.setNotes(previousOwner == null ? "Prva registracija vozila" : "Prijenos vlasništva");
        ownershipHistoryRepository.save(history);
    }
}