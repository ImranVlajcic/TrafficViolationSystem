package com.academy.trafficviolationsystem.core.config.seed;

import com.academy.trafficviolationsystem.user.UserEntity;
import com.academy.trafficviolationsystem.user.UserRepository;
import com.academy.trafficviolationsystem.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds:
 *  - 1 admin  (admin / admin123)               — preserved from the original seeder
 *  - 1 named officer (officer / officer123)    — preserved from the original seeder
 *  - 14 additional officers (officerN / officer123)
 *  - 90 citizen accounts (citizenN / citizen123)
 *
 * Returned lists let DriverSeeder link ~70 of the citizens 1:1 to a DriverEntity,
 * and let ViolationSeeder/AppealSeeder pick random officers as actors.
 */
@Component
public class UserSeeder {

    private static final int OFFICER_COUNT = 15; // includes the named "officer" account
    private static final int CITIZEN_COUNT = 90;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static class SeededUsers {
        public UserEntity admin;
        public List<UserEntity> officers = new ArrayList<>();
        public List<UserEntity> citizens = new ArrayList<>();
    }

    public SeededUsers seed() {
        SeededUsers result = new SeededUsers();

        result.admin = userRepository.findByUsername("admin").orElseGet(() -> {
            UserEntity admin = new UserEntity();
            admin.setUsername("admin");
            admin.setEmail("admin@traffic-academy.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setRole(UserRole.ADMIN);
            admin.setActive(true);
            UserEntity saved = userRepository.save(admin);
            System.out.println("======== Default Admin Created! ========");
            System.out.println("Username: admin");
            System.out.println("Password: admin123");
            System.out.println("========================================");
            return saved;
        });

        UserEntity namedOfficer = userRepository.findByUsername("officer").orElseGet(() -> {
            UserEntity officer = new UserEntity();
            officer.setUsername("officer");
            officer.setEmail("officer@traffic-academy.com");
            officer.setPasswordHash(passwordEncoder.encode("officer123"));
            officer.setFirstName("John");
            officer.setLastName("Officer");
            officer.setRole(UserRole.OFFICER);
            officer.setBadgeNumber(SeedConstants.BADGE_PREFIX + "0001");
            officer.setActive(true);
            UserEntity saved = userRepository.save(officer);
            System.out.println("======== Default Officer Created! ========");
            System.out.println("Username: officer");
            System.out.println("Password: officer123");
            System.out.println("==========================================");
            return saved;
        });
        result.officers.add(namedOfficer);

        if (userRepository.count() > 2) {
            // Additional users already seeded in a previous run — reload and return.
            result.officers.addAll(userRepository.findAllByRole(UserRole.OFFICER)
                    .stream().filter(o -> !o.getUsername().equals("officer")).toList());
            result.citizens.addAll(userRepository.findAllByRole(UserRole.CITIZEN));
            return result;
        }

        for (int i = 2; i <= OFFICER_COUNT; i++) {
            boolean male = SeedRandom.chance(0.7);
            String first = male ? SeedRandom.pick(SeedConstants.MALE_FIRST_NAMES)
                    : SeedRandom.pick(SeedConstants.FEMALE_FIRST_NAMES);
            String last = SeedRandom.pick(SeedConstants.LAST_NAMES);

            UserEntity officer = new UserEntity();
            officer.setUsername("officer" + i);
            officer.setEmail("officer" + i + "@traffic-academy.com");
            officer.setPasswordHash(passwordEncoder.encode("officer123"));
            officer.setFirstName(first);
            officer.setLastName(last);
            officer.setPhoneNumber("061" + SeedRandom.digits(6));
            officer.setRole(UserRole.OFFICER);
            officer.setBadgeNumber(SeedConstants.BADGE_PREFIX + String.format("%04d", i));
            officer.setActive(SeedRandom.chance(0.95));
            result.officers.add(userRepository.save(officer));
        }

        for (int i = 1; i <= CITIZEN_COUNT; i++) {
            boolean male = SeedRandom.chance(0.55);
            String first = male ? SeedRandom.pick(SeedConstants.MALE_FIRST_NAMES)
                    : SeedRandom.pick(SeedConstants.FEMALE_FIRST_NAMES);
            String last = SeedRandom.pick(SeedConstants.LAST_NAMES);

            UserEntity citizen = new UserEntity();
            citizen.setUsername("citizen" + i);
            citizen.setEmail("citizen" + i + "@example.com");
            citizen.setPasswordHash(passwordEncoder.encode("citizen123"));
            citizen.setFirstName(first);
            citizen.setLastName(last);
            citizen.setPhoneNumber("062" + SeedRandom.digits(6));
            citizen.setRole(UserRole.CITIZEN);
            citizen.setActive(SeedRandom.chance(0.97));
            result.citizens.add(userRepository.save(citizen));
        }

        System.out.println("Seeded " + result.officers.size() + " officers and "
                + result.citizens.size() + " citizen accounts (password for all: officer123 / citizen123).");

        return result;
    }
}