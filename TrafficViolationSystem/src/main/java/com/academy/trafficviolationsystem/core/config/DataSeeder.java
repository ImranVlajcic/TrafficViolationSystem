package com.academy.trafficviolationsystem.core.config;

import com.academy.trafficviolationsystem.user.UserEntity;
import com.academy.trafficviolationsystem.user.UserRepository;
import com.academy.trafficviolationsystem.user.UserRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Data seeder for first time database startup
 * Creates an admin user to access protected API calls
 */

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository,
                                          PasswordEncoder passwordEncoder) {
        return args -> {

            if (userRepository.findByUsername("admin").isEmpty()) {

                UserEntity admin = new UserEntity();

                admin.setUsername("admin");
                admin.setEmail("admin@traffic-academy.com");
                admin.setPasswordHash(passwordEncoder.encode("admin123"));

                admin.setFirstName("System");
                admin.setLastName("Administrator");

                admin.setRole(UserRole.ADMIN);
                admin.setActive(true);

                userRepository.save(admin);

                System.out.println("======== Default Admin Created! ========");
                System.out.println("Username: admin");
                System.out.println("Password: admin123");
                System.out.println("========================================");
            }

            if (userRepository.findByUsername("officer").isEmpty()) {

                UserEntity officer = new UserEntity();

                officer.setUsername("officer");
                officer.setEmail("officer@traffic-academy.com");
                officer.setPasswordHash(passwordEncoder.encode("officer123"));

                officer.setFirstName("John");
                officer.setLastName("Officer");

                officer.setRole(UserRole.OFFICER);
                officer.setActive(true);

                userRepository.save(officer);

                System.out.println("======== Default Officer Created! ========");
                System.out.println("Username: officer");
                System.out.println("Password: officer123");
                System.out.println("==========================================");
            }
        };
    }
}