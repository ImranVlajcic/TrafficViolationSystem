package com.academy.trafficviolationsystem.core.config.seed;

import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.driver.DriverRepository;
import com.academy.trafficviolationsystem.user.UserEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds 90 drivers. The first 70 are linked 1:1 to the citizen UserEntity
 * of the same index (citizen portal accounts); the remaining 20 are
 * officer-registered drivers with no portal account (user = null) — mirrors
 * the real system where an officer can register a driver who hasn't signed
 * up for the citizen portal.
 *
 * penaltyPoints/isSuspended are left at their defaults here — FineSeeder
 * updates them afterwards as it processes violations chronologically.
 */
@Component
public class DriverSeeder {

    private static final int DRIVER_COUNT = 90;
    private static final int LINKED_TO_CITIZEN_COUNT = 70;

    private final DriverRepository driverRepository;

    public DriverSeeder(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public List<DriverEntity> seed(List<UserEntity> citizens) {
        if (driverRepository.count() > 0) {
            return driverRepository.findAll();
        }

        List<DriverEntity> drivers = new ArrayList<>();

        for (int i = 0; i < DRIVER_COUNT; i++) {
            boolean linked = i < LINKED_TO_CITIZEN_COUNT && i < citizens.size();
            UserEntity linkedUser = linked ? citizens.get(i) : null;

            LocalDate dob = LocalDate.now().minusYears(SeedRandom.intBetween(19, 68))
                    .minusDays(SeedRandom.intBetween(0, 364));
            LocalDate licenseIssued = dob.plusYears(SeedRandom.intBetween(18, 25))
                    .plusDays(SeedRandom.intBetween(0, 300));
            // A handful of licenses (~8%) are already expired, to exercise
            // the LICENSE_EXPIRY_WARNING / expired-driver frontend states.
            LocalDate licenseExpires = SeedRandom.chance(0.08)
                    ? LocalDate.now().minusDays(SeedRandom.intBetween(1, 90))
                    : licenseIssued.plusYears(10);

            DriverEntity driver = new DriverEntity();
            driver.setLicenseNumber(String.format("DL-%06d", i + 1));
            driver.setNationalId(SeedRandom.digits(13));

            if (linkedUser != null) {
                driver.setFirstName(linkedUser.getFirstName());
                driver.setLastName(linkedUser.getLastName());
                driver.setEmail(linkedUser.getEmail());
                driver.setPhoneNumber(linkedUser.getPhoneNumber());
            } else {
                boolean male = SeedRandom.chance(0.6);
                driver.setFirstName(male ? SeedRandom.pick(SeedConstants.MALE_FIRST_NAMES)
                        : SeedRandom.pick(SeedConstants.FEMALE_FIRST_NAMES));
                driver.setLastName(SeedRandom.pick(SeedConstants.LAST_NAMES));
                driver.setEmail(driver.getFirstName().toLowerCase() + "."
                        + driver.getLastName().toLowerCase() + i + "@mail.com");
                driver.setPhoneNumber("063" + SeedRandom.digits(6));
            }

            driver.setDateOfBirth(dob);
            String city = SeedRandom.pick(SeedConstants.CITIES);
            String street = SeedRandom.pick(SeedConstants.STREETS);
            driver.setAddress(street + " " + SeedRandom.intBetween(1, 120) + ", " + city);
            driver.setLicenseCategory(SeedRandom.pick(SeedConstants.LICENSE_CATEGORIES));
            driver.setLicenseIssuedAt(licenseIssued);
            driver.setLicenseExpiresAt(licenseExpires);
            driver.setPenaltyPoints(0);
            driver.setSuspended(false);
            driver.setSuspendedUntil(null);
            driver.setUser(linkedUser);

            drivers.add(driverRepository.save(driver));
        }

        return drivers;
    }
}